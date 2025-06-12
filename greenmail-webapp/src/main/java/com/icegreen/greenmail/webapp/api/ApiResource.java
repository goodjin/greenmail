package com.icegreen.greenmail.webapp.api;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.webapp.ContextHelper;
import com.icegreen.greenmail.webapp.api.dto.MailboxApiModel;
import com.icegreen.greenmail.webapp.api.dto.UserApiModel;
import com.icegreen.greenmail.webapp.api.dto.EmailListItemApiModel;
import com.icegreen.greenmail.webapp.api.dto.EmailDetailApiModel; // New DTO
import com.icegreen.greenmail.webapp.api.dto.HeaderApiModel; // New DTO
import com.icegreen.greenmail.webapp.api.dto.AttachmentApiModel; // New DTO
import com.icegreen.greenmail.store.StoredMessage;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Header;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;


import jakarta.servlet.ServletContext;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat; // For date formatting
import java.util.ArrayList; // For initializing lists
import java.util.Arrays; // For stream from array
import java.util.Collections; // For empty list
import java.util.Date; // For dates
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class ApiResource {
    // ISO 8601 date format
    private static final String DATE_FORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_ISO8601);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private List<String> formatAddresses(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(addresses)
                .map(Address::toString)
                .collect(Collectors.toList());
    }

    // Updated hasAttachments to be more robust by checking filename too
    private boolean hasAttachments(Part part) throws MessagingException, IOException {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) && part.getFileName() != null) {
            return true;
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                if (hasAttachments(multipart.getBodyPart(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void extractBodyParts(Part part, StringBuilder textBody, StringBuilder htmlBody) throws MessagingException, IOException {
        if (part.isMimeType("text/plain") && htmlBody.length() == 0 && textBody.length() == 0) { // prefer html if available
            textBody.append((String) part.getContent());
        } else if (part.isMimeType("text/html")) {
            htmlBody.append((String) part.getContent());
        } else if (part.isMimeType("multipart/alternative")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                extractBodyParts(multipart.getBodyPart(i), textBody, htmlBody);
            }
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                 // Only recurse if not an attachment, to avoid processing attachment content as body
                BodyPart subPart = multipart.getBodyPart(i);
                if (!Part.ATTACHMENT.equalsIgnoreCase(subPart.getDisposition())) {
                    extractBodyParts(subPart, textBody, htmlBody);
                }
            }
        }
    }

    private List<AttachmentApiModel> extractAttachments(Part part, String userEmail, String mailboxId, String emailId) throws MessagingException, IOException {
        List<AttachmentApiModel> attachments = new ArrayList<>();
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) || bodyPart.getFileName() != null) {
                    AttachmentApiModel att = new AttachmentApiModel();
                    String filename = bodyPart.getFileName();
                    if (filename != null) {
                        try {
                           filename = MimeUtility.decodeText(filename); // Decode filename
                        } catch (Exception e) {
                            // Keep original if decoding fails
                            LOGGER.warn("Failed to decode attachment filename: {}", filename, e);
                        }
                    } else {
                        // Try to generate a filename from content type if actual filename is null
                        String[] contentType = bodyPart.getHeader("Content-Type");
                        if (contentType != null && contentType.length > 0) {
                            filename = "attachment." + MimeUtility.getExtension(contentType[0], "bin");
                        } else {
                            filename = "attachment.bin";
                        }
                    }
                    att.setFilename(filename);
                    att.setContentType(bodyPart.getContentType());
                    att.setSize(bodyPart.getSize()); // May be -1 if not known
                    att.setContentId(bodyPart.getHeader("Content-ID") != null ? bodyPart.getHeader("Content-ID")[0] : null);

                    // Construct download URL (URL encoding for filename)
                    String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20");
                    att.setDownloadUrl(String.format("/api/users/%s/mailboxes/%s/emails/%s/attachments/%s",
                            userEmail, mailboxId, emailId, encodedFilename));
                    attachments.add(att);
                } else if (bodyPart.isMimeType("multipart/*")) { // Recurse for nested multiparts
                    attachments.addAll(extractAttachments(bodyPart, userEmail, mailboxId, emailId));
                }
            }
        }
        return attachments;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiResource.class);

    @Context
    private ServletContext context;

    private Managers getManagers() {
        return ContextHelper.getManagers(context);
    }

    @GET
    @Path("/users")
    public Response getUsers() {
        try {
            UserManager userManager = getManagers().getUserManager();
            List<GreenMailUser> users = userManager.listUser();
            List<UserApiModel> userApiModels = users.stream()
                    .map(user -> new UserApiModel(user.getEmail()))
                    .collect(Collectors.toList());
            return Response.ok(userApiModels).build();
        } catch (Exception e) {
            LOGGER.error("Error getting users", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error getting users: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/users/{userEmail}/mailboxes")
    public Response getMailboxes(@PathParam("userEmail") String userEmail) {
        try {
            UserManager userManager = getManagers().getUserManager();
            GreenMailUser user = userManager.getUserByEmail(userEmail);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("User not found: " + userEmail).build();
            }

            ImapHostManager imapHostManager = getManagers().getImapHostManager();
            List<MailboxApiModel> mailboxes = new java.util.ArrayList<>();

            try {
                // GreenMail automatically creates INBOX for a user upon first access if it doesn't exist.
                MailFolder inbox = imapHostManager.getInbox(user);
                mailboxes.add(new MailboxApiModel(inbox.getFullName(), inbox.getName(), userEmail));

                // To list other mailboxes, one might iterate through:
                // List<MailFolder> allUserFolders = imapHostManager.listMailboxes(user, user.getQualifiedMailboxName() + "/*", false);
                // For now, only INBOX is primary as per requirements.

            } catch (FolderException e) {
                LOGGER.error("Error accessing mailboxes for user {}", userEmail, e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                               .entity("Error retrieving mailboxes: " + e.getMessage()).build();
            }

            return Response.ok(mailboxes).build();
        } catch (UserException e) { // Catch specific UserException for user lookup issues
             LOGGER.warn("User not found for email {}", userEmail, e);
             return Response.status(Response.Status.NOT_FOUND)
                            .entity("User not found: " + userEmail).build();
        } catch (Exception e) {
            LOGGER.error("Error getting mailboxes for user {}", userEmail, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error getting mailboxes: " + e.getMessage()).build();
        }
    }

    // Other endpoints will be added here

    @GET
    @Path("/users/{userEmail}/mailboxes/{mailboxId}/emails")
    public Response getEmails(
            @PathParam("userEmail") String userEmail,
            @PathParam("mailboxId") String mailboxId) {
        try {
            UserManager userManager = getManagers().getUserManager();
            GreenMailUser user = userManager.getUserByEmail(userEmail);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("User not found: " + userEmail).build();
            }

            ImapHostManager imapHostManager = getManagers().getImapHostManager();
            MailFolder mailFolder = imapHostManager.getFolder(user, mailboxId);
            if (mailFolder == null || !mailFolder.exists()) {
                mailFolder = imapHostManager.getFolder(mailboxId); // Try full path
                 if (mailFolder == null || !mailFolder.exists() ||
                     !mailFolder.getFullName().toLowerCase().startsWith(user.getQualifiedMailboxName().toLowerCase()) && // Check ownership
                     !mailFolder.getFullName().equalsIgnoreCase(mailboxId)) { // if mailboxId was full path
                    return Response.status(Response.Status.NOT_FOUND)
                                   .entity("Mailbox not found: " + mailboxId).build();
                }
            }

            List<StoredMessage> storedMessages = mailFolder.getMessages();
            List<EmailListItemApiModel> emailList = new ArrayList<>();

            for (StoredMessage storedMessage : storedMessages) {
                MimeMessage mimeMessage = storedMessage.getMimeMessage();
                EmailListItemApiModel item = new EmailListItemApiModel();
                item.setId(String.valueOf(storedMessage.getUid()));
                item.setSubject(mimeMessage.getSubject());
                item.setFrom(mimeMessage.getFrom() != null && mimeMessage.getFrom().length > 0 ? mimeMessage.getFrom()[0].toString() : "");
                item.setTo(formatAddresses(mimeMessage.getRecipients(Message.RecipientType.TO)));
                item.setCc(formatAddresses(mimeMessage.getRecipients(Message.RecipientType.CC)));
                item.setBcc(formatAddresses(mimeMessage.getRecipients(Message.RecipientType.BCC)));
                item.setSentDate(formatDate(mimeMessage.getSentDate()));
                item.setReceivedDate(formatDate(storedMessage.getReceivedDate())); // Use StoredMessage.getReceivedDate()
                item.setSize(mimeMessage.getSize());
                item.setUnread(!mimeMessage.isSet(Flags.Flag.SEEN));
                item.setHasAttachments(hasAttachments(mimeMessage));
                emailList.add(item);
            }

            return Response.ok(emailList).build();
        } catch (UserException e) {
            LOGGER.warn("User not found for email {} when getting emails", userEmail, e);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("User not found: " + userEmail).build();
        } catch (FolderException e) {
            LOGGER.warn("Mailbox not found {} for user {}", mailboxId, userEmail, e);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("Mailbox not found: " + mailboxId).build();
        } catch (MessagingException | IOException e) {
            LOGGER.error("Error processing emails for mailbox {} of user {}", mailboxId, userEmail, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error processing emails: " + e.getMessage()).build();
        } catch (Exception e) {
            LOGGER.error("General error getting emails for mailbox {} of user {}", mailboxId, userEmail, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error getting emails: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/users/{userEmail}/mailboxes/{mailboxId}/emails/{emailId}")
    public Response getEmail(
            @PathParam("userEmail") String userEmail,
            @PathParam("mailboxId") String mailboxId,
            @PathParam("emailId") long emailUid) { // emailId is UID
        try {
            UserManager userManager = getManagers().getUserManager();
            GreenMailUser user = userManager.getUserByEmail(userEmail);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found: " + userEmail).build();
            }

            ImapHostManager imapHostManager = getManagers().getImapHostManager();
            MailFolder mailFolder = imapHostManager.getFolder(user, mailboxId);
             if (mailFolder == null || !mailFolder.exists()) {
                mailFolder = imapHostManager.getFolder(mailboxId); // Try full path
                 if (mailFolder == null || !mailFolder.exists() ||
                     !mailFolder.getFullName().toLowerCase().startsWith(user.getQualifiedMailboxName().toLowerCase()) &&
                     !mailFolder.getFullName().equalsIgnoreCase(mailboxId)) {
                    return Response.status(Response.Status.NOT_FOUND).entity("Mailbox not found: " + mailboxId).build();
                }
            }

            StoredMessage storedMessage = mailFolder.getMessageByUid(emailUid);
            if (storedMessage == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Email not found with UID: " + emailUid).build();
            }

            MimeMessage mimeMessage = storedMessage.getMimeMessage();
            EmailDetailApiModel detail = new EmailDetailApiModel();
            detail.setId(String.valueOf(storedMessage.getUid()));
            String[] messageIdHeaders = mimeMessage.getHeader("Message-ID");
            if(messageIdHeaders != null && messageIdHeaders.length > 0) {
                detail.setMessageIdHeader(messageIdHeaders[0]);
            }
            detail.setSubject(mimeMessage.getSubject());
            detail.setFrom(mimeMessage.getFrom() != null && mimeMessage.getFrom().length > 0 ? mimeMessage.getFrom()[0].toString() : "");
            detail.setTo(formatAddresses(mimeMessage.getRecipients(Message.RecipientType.TO)));
            detail.setCc(formatAddresses(mimeMessage.getRecipients(Message.RecipientType.CC)));
            detail.setBcc(formatAddresses(mimeMessage.getRecipients(Message.RecipientType.BCC)));
            detail.setSentDate(formatDate(mimeMessage.getSentDate()));
            detail.setReceivedDate(formatDate(storedMessage.getReceivedDate()));
            detail.setSize(mimeMessage.getSize());
            detail.setUnread(!mimeMessage.isSet(Flags.Flag.SEEN));

            List<HeaderApiModel> headers = new ArrayList<>();
            Collections.list(mimeMessage.getAllHeaders()).forEach(h -> headers.add(new HeaderApiModel(h.getName(), h.getValue())));
            detail.setHeaders(headers);

            StringBuilder textBody = new StringBuilder();
            StringBuilder htmlBody = new StringBuilder();
            extractBodyParts(mimeMessage, textBody, htmlBody);
            detail.setBodyText(textBody.length() > 0 ? textBody.toString() : null);
            detail.setBodyHtml(htmlBody.length() > 0 ? htmlBody.toString() : null);

            detail.setAttachments(extractAttachments(mimeMessage, userEmail, mailboxId, String.valueOf(emailUid)));

            ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
            mimeMessage.writeTo(rawOut);
            detail.setRaw(rawOut.toString(StandardCharsets.UTF_8.name())); // Consider charsets

            return Response.ok(detail).build();
        } catch (UserException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found: " + userEmail).build();
        } catch (FolderException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Mailbox not found: " + mailboxId).build();
        } catch (MessagingException | IOException e) {
            LOGGER.error("Error processing email UID {} for mailbox {} of user {}", emailUid, mailboxId, userEmail, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error processing email: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/users/{userEmail}/mailboxes/{mailboxId}/emails/{emailId}/attachments/{attachmentFilename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM) // Default, but can be overridden
    public Response getAttachment(
            @PathParam("userEmail") String userEmail,
            @PathParam("mailboxId") String mailboxId,
            @PathParam("emailId") long emailUid,
            @PathParam("attachmentFilename") String attachmentFilename) {
        try {
            UserManager userManager = getManagers().getUserManager();
            GreenMailUser user = userManager.getUserByEmail(userEmail);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found: " + userEmail).build();
            }

            ImapHostManager imapHostManager = getManagers().getImapHostManager();
            MailFolder mailFolder = imapHostManager.getFolder(user, mailboxId);
            if (mailFolder == null || !mailFolder.exists()) {
                 mailFolder = imapHostManager.getFolder(mailboxId); // Try full path
                 if (mailFolder == null || !mailFolder.exists() ||
                     !mailFolder.getFullName().toLowerCase().startsWith(user.getQualifiedMailboxName().toLowerCase()) &&
                     !mailFolder.getFullName().equalsIgnoreCase(mailboxId)) {
                    return Response.status(Response.Status.NOT_FOUND).entity("Mailbox not found: " + mailboxId).build();
                }
            }

            StoredMessage storedMessage = mailFolder.getMessageByUid(emailUid);
            if (storedMessage == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Email not found with UID: " + emailUid).build();
            }

            MimeMessage mimeMessage = storedMessage.getMimeMessage();
            Object content = mimeMessage.getContent();

            if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    String partFilename = bodyPart.getFileName();

                    if (partFilename != null) {
                        try {
                            partFilename = MimeUtility.decodeText(partFilename);
                        } catch (Exception e) {
                            LOGGER.warn("Failed to decode attachment filename for comparison: {}", partFilename, e);
                        }
                        if (attachmentFilename.equals(partFilename)) {
                            StreamingOutput stream = output -> {
                                bodyPart.getDataHandler().writeTo(output);
                                output.flush();
                            };
                            // Ensure original filename (decoded) is used in Content-Disposition
                            String decodedAttachmentFilename = MimeUtility.decodeText(bodyPart.getFileName());
                            return Response.ok(stream)
                                    .type(bodyPart.getContentType())
                                    .header("Content-Disposition", "attachment; filename=\"" + decodedAttachmentFilename + "\"")
                                    .build();
                        }
                    }
                }
            } else if (Part.ATTACHMENT.equalsIgnoreCase(mimeMessage.getDisposition()) || mimeMessage.getFileName() != null) {
                // Handle case where the message itself is an attachment (less common for multi-part emails)
                String partFilename = mimeMessage.getFileName();
                 if (partFilename != null) {
                        try {
                            partFilename = MimeUtility.decodeText(partFilename);
                        } catch (Exception e) {
                             LOGGER.warn("Failed to decode attachment filename for comparison: {}", partFilename, e);
                        }
                    if (attachmentFilename.equals(partFilename)) {
                        StreamingOutput stream = output -> {
                            mimeMessage.getDataHandler().writeTo(output);
                            output.flush();
                        };
                        String decodedAttachmentFilename = MimeUtility.decodeText(mimeMessage.getFileName());
                        return Response.ok(stream)
                                .type(mimeMessage.getContentType())
                                .header("Content-Disposition", "attachment; filename=\"" + decodedAttachmentFilename + "\"")
                                .build();
                    }
                }
            }


            return Response.status(Response.Status.NOT_FOUND).entity("Attachment not found: " + attachmentFilename).build();
        } catch (UserException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found: " + userEmail).build();
        } catch (FolderException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Mailbox not found: " + mailboxId).build();
        } catch (MessagingException | IOException e) {
            LOGGER.error("Error retrieving attachment {} for email UID {}", attachmentFilename, emailUid, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error retrieving attachment: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/users/{userEmail}/mailboxes/{mailboxId}/emails/{emailId}")
    public Response deleteEmail(
            @PathParam("userEmail") String userEmail,
            @PathParam("mailboxId") String mailboxId,
            @PathParam("emailId") long emailUid) {
        try {
            UserManager userManager = getManagers().getUserManager();
            GreenMailUser user = userManager.getUserByEmail(userEmail);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found: " + userEmail).build();
            }

            ImapHostManager imapHostManager = getManagers().getImapHostManager();
            MailFolder mailFolder = imapHostManager.getFolder(user, mailboxId);
            if (mailFolder == null || !mailFolder.exists()) {
                 mailFolder = imapHostManager.getFolder(mailboxId); // Try full path
                 if (mailFolder == null || !mailFolder.exists() ||
                     !mailFolder.getFullName().toLowerCase().startsWith(user.getQualifiedMailboxName().toLowerCase()) &&
                     !mailFolder.getFullName().equalsIgnoreCase(mailboxId)) {
                    return Response.status(Response.Status.NOT_FOUND).entity("Mailbox not found: " + mailboxId).build();
                }
            }

            StoredMessage storedMessage = mailFolder.getMessageByUid(emailUid);
            if (storedMessage == null) {
                // If message doesn't exist, it's effectively gone. Some might argue for 204, but 404 is also common.
                return Response.status(Response.Status.NOT_FOUND).entity("Email not found with UID: " + emailUid).build();
            }

            // Mark the message as DELETED and expunge.
            // GreenMail's MailFolder.deleteMessages(long[] uids) might be simpler.
            // Let's verify its behavior. It sets DELETED flag and calls expunge.
            mailFolder.deleteMessages(new long[]{emailUid});
            // mailFolder.expunge() might be needed if deleteMessages only flags.
            // From MailFolder.deleteMessages: "Deletes the messages specified by the given UIDs ... also expunges the deleted messages"
            // So, expunge is not needed separately.

            return Response.noContent().build(); // 204 No Content for successful deletion
        } catch (UserException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found: " + userEmail).build();
        } catch (FolderException e) {
            LOGGER.error("Error deleting email UID {} from mailbox {} for user {}", emailUid, mailboxId, userEmail, e);
            // This could be 404 if mailbox not found, or 500 if other folder issue
            return Response.status(Response.Status.NOT_FOUND).entity("Mailbox not found or error during deletion: " + mailboxId).build();
        } catch (Exception e) { // Catch any other unexpected errors
            LOGGER.error("Error deleting email UID {} for mailbox {} of user {}", emailUid, mailboxId, userEmail, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error deleting email: " + e.getMessage()).build();
        }
    }
}
