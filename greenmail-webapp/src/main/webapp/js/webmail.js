document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = '/greenmail/api'; // Adjust if your base path is different

    // Panes
    const usersPane = document.getElementById('users-pane');
    const emailsPane = document.getElementById('emails-pane');
    const emailViewPane = document.getElementById('email-view-pane');

    // Lists
    const usersList = document.getElementById('users-list');
    const emailsList = document.getElementById('emails-list');
    const attachmentsList = document.getElementById('attachments-list');

    // Email view elements
    const emailContent = document.getElementById('email-content');
    const emailPlaceholder = document.getElementById('email-placeholder');
    const emailHeadersDiv = document.getElementById('email-headers');
    const emailBodyHtmlFrame = document.getElementById('email-body-html');
    const emailBodyTextPre = document.getElementById('email-body-text');
    const deleteEmailButton = document.getElementById('delete-email-button');
    const currentMailboxSpan = document.getElementById('current-mailbox');


    let selectedUserEmail = null;
    let selectedMailboxId = null;
    let selectedEmailId = null;

    // --- API Helper ---
    async function apiFetch(url, options = {}) {
        try {
            const response = await fetch(API_BASE_URL + url, options);
            if (!response.ok) {
                const errorText = await response.text();
                console.error(`API Error ${response.status}: ${errorText} for URL: ${url}`);
                alert(`Error: ${errorText || response.statusText}`);
                return null;
            }
            if (response.status === 204) { // No Content
                return true;
            }
            return response.json();
        } catch (error) {
            console.error('Network or other error:', error);
            alert('Network error. See console for details.');
            return null;
        }
    }

    // --- Rendering Functions ---
    function renderUsers(users) {
        usersList.innerHTML = ''; // Clear existing
        if (!users || users.length === 0) {
            usersList.innerHTML = '<li>No users found.</li>';
            return;
        }
        users.forEach(user => {
            const li = document.createElement('li');
            li.textContent = user.email;
            li.dataset.email = user.email;
            li.addEventListener('click', () => handleUserClick(user.email));
            usersList.appendChild(li);
        });
    }

    function renderMailboxes(mailboxes) {
        // For now, mailboxes are also displayed in the users-pane, below the user
        // Or, could replace user list with mailbox list, or use a dedicated mailbox list element.
        // Let's find the clicked user li and append mailboxes there or clear and show only mailboxes.
        // For simplicity, let's assume a user only has one main list of mailboxes shown at a time.

        // Clear previous mailboxes if any, or integrate into user list items
        const userLi = usersList.querySelector(`li[data-email="${selectedUserEmail}"].active`);
        if (!userLi) return;

        let mailboxesUl = userLi.querySelector('ul.mailboxes-list');
        if (!mailboxesUl) {
            mailboxesUl = document.createElement('ul');
            mailboxesUl.className = 'mailboxes-list'; // For potential styling
            userLi.appendChild(mailboxesUl);
        }
        mailboxesUl.innerHTML = ''; // Clear existing mailboxes for this user

        if (!mailboxes || mailboxes.length === 0) {
            mailboxesUl.innerHTML = '<li>No mailboxes found.</li>';
            return;
        }
        mailboxes.forEach(mailbox => {
            const li = document.createElement('li');
            li.textContent = mailbox.name; // e.g., INBOX
            li.dataset.mailboxId = mailbox.id; // e.g., user@example.com#INBOX
            li.dataset.userEmail = mailbox.userEmail; // Store for context
            li.style.marginLeft = '20px'; // Indent mailboxes
            li.addEventListener('click', (event) => {
                event.stopPropagation(); // Prevent user click event from re-triggering
                handleMailboxClick(mailbox.userEmail, mailbox.id, li);
            });
            mailboxesUl.appendChild(li);
        });
    }

    function renderEmails(emails) {
        emailsList.innerHTML = '';
        if (!emails || emails.length === 0) {
            emailsList.innerHTML = '<li>No emails in this mailbox.</li>';
            return;
        }
        emails.forEach(email => {
            const li = document.createElement('li');
            li.className = 'email-item';
            if (email.unread) {
                li.classList.add('unread');
            }
            li.dataset.emailId = email.id;

            const subjectDiv = document.createElement('div');
            subjectDiv.className = 'subject';
            subjectDiv.textContent = email.subject || '(No Subject)';

            const fromDiv = document.createElement('div');
            fromDiv.className = 'from';
            fromDiv.textContent = `From: ${email.from || 'N/A'}`;

            const dateDiv = document.createElement('div');
            dateDiv.className = 'date';
            dateDiv.textContent = `Date: ${email.sentDate ? new Date(email.sentDate).toLocaleString() : 'N/A'}`;

            li.appendChild(subjectDiv);
            li.appendChild(fromDiv);
            li.appendChild(dateDiv);

            li.addEventListener('click', () => handleEmailClick(email.id, li));
            emailsList.appendChild(li);
        });
    }

    function renderEmailDetail(email) {
        if (!email) {
            emailContent.style.display = 'none';
            emailPlaceholder.style.display = 'block';
            deleteEmailButton.style.display = 'none';
            return;
        }
        emailPlaceholder.style.display = 'none';
        emailContent.style.display = 'block';
        deleteEmailButton.style.display = 'inline-block';
        selectedEmailId = email.id; // Store for delete button

        let headersHtml = `
            <p><strong>From:</strong> ${email.from || ''}</p>
            <p><strong>To:</strong> ${(email.to || []).join(', ')}</p>
        `;
        if (email.cc && email.cc.length > 0) {
            headersHtml += `<p><strong>Cc:</strong> ${email.cc.join(', ')}</p>`;
        }
        if (email.bcc && email.bcc.length > 0) { // BCC might not be available from server
            headersHtml += `<p><strong>Bcc:</strong> ${email.bcc.join(', ')}</p>`;
        }
        headersHtml += `<p><strong>Subject:</strong> ${email.subject || '(No Subject)'}</p>`;
        headersHtml += `<p><strong>Date:</strong> ${email.sentDate ? new Date(email.sentDate).toLocaleString() : ''}</p>`;
        if (email.messageIdHeader) {
            headersHtml += `<p><strong>Message-ID:</strong> ${email.messageIdHeader}</p>`;
        }
        emailHeadersDiv.innerHTML = headersHtml;

        if (email.bodyHtml) {
            emailBodyHtmlFrame.style.display = 'block';
            emailBodyTextPre.style.display = 'none';
            // Use srcdoc to set iframe content for security and simplicity
            emailBodyHtmlFrame.srcdoc = email.bodyHtml;
        } else if (email.bodyText) {
            emailBodyHtmlFrame.style.display = 'none';
            emailBodyTextPre.style.display = 'block';
            emailBodyTextPre.textContent = email.bodyText;
        } else {
            emailBodyHtmlFrame.style.display = 'none';
            emailBodyTextPre.style.display = 'block';
            emailBodyTextPre.textContent = '(No message body)';
        }

        attachmentsList.innerHTML = '';
        if (email.attachments && email.attachments.length > 0) {
            email.attachments.forEach(att => {
                const li = document.createElement('li');
                const a = document.createElement('a');
                // API_BASE_URL is not needed here as downloadUrl from API should be absolute or root-relative
                a.href = att.downloadUrl;
                a.textContent = `${att.filename} (${att.contentType}, ${att.size} bytes)`;
                // a.target = '_blank'; // Open in new tab
                li.appendChild(a);
                attachmentsList.appendChild(li);
            });
        } else {
            attachmentsList.innerHTML = '<li>No attachments.</li>';
        }
    }

    // --- Event Handlers ---
    async function handleUserClick(userEmail) {
        selectedUserEmail = userEmail;
        selectedMailboxId = null; // Reset mailbox
        selectedEmailId = null; // Reset email
        emailsList.innerHTML = '<li>Select a mailbox.</li>';
        renderEmailDetail(null); // Clear email view
        currentMailboxSpan.textContent = '';


        // Highlight active user
        usersList.querySelectorAll('li').forEach(li => li.classList.remove('active'));
        const userLi = usersList.querySelector(`li[data-email="${userEmail}"]`);
        if (userLi) userLi.classList.add('active');

        // Remove mailboxes from other users
        usersList.querySelectorAll('ul.mailboxes-list').forEach(ul => {
            if (ul.parentElement !== userLi) {
                ul.innerHTML = '';
            }
        });


        const mailboxes = await apiFetch(`/users/${userEmail}/mailboxes`);
        if (mailboxes) {
            renderMailboxes(mailboxes);
        }
    }

    async function handleMailboxClick(userEmail, mailboxId, mailboxLiElement) {
        selectedMailboxId = mailboxId;
        selectedEmailId = null; // Reset email
        renderEmailDetail(null); // Clear email view
        currentMailboxSpan.textContent = `Mailbox: ${mailboxId.substring(mailboxId.indexOf('#') + 1)}`;


        // Highlight active mailbox
        const userLi = usersList.querySelector(`li[data-email="${userEmail}"].active`);
        if (userLi) {
            userLi.querySelectorAll('ul.mailboxes-list li').forEach(li => li.classList.remove('active'));
        }
        if (mailboxLiElement) mailboxLiElement.classList.add('active');

        const emails = await apiFetch(`/users/${userEmail}/mailboxes/${mailboxId}/emails`);
        if (emails) {
            renderEmails(emails);
        }
    }

    async function handleEmailClick(emailId, emailLiElement) {
        if (!selectedUserEmail || !selectedMailboxId) return;
        selectedEmailId = emailId;

        // Highlight active email
        emailsList.querySelectorAll('li').forEach(li => li.classList.remove('active'));
        if(emailLiElement) emailLiElement.classList.add('active');


        const email = await apiFetch(`/users/${selectedUserEmail}/mailboxes/${selectedMailboxId}/emails/${emailId}`);
        if (email) {
            renderEmailDetail(email);
            // Mark as read (visually, actual flag setting is more complex and not implemented here)
            if(emailLiElement) emailLiElement.classList.remove('unread');
        }
    }

    deleteEmailButton.addEventListener('click', async () => {
        if (!selectedUserEmail || !selectedMailboxId || !selectedEmailId) {
            alert('No email selected to delete.');
            return;
        }
        if (!confirm('Are you sure you want to delete this email?')) {
            return;
        }

        const success = await apiFetch(`/users/${selectedUserEmail}/mailboxes/${selectedMailboxId}/emails/${selectedEmailId}`, {
            method: 'DELETE'
        });

        if (success) {
            alert('Email deleted successfully.');
            renderEmailDetail(null); // Clear view
            // Refresh email list
            const emails = await apiFetch(`/users/${selectedUserEmail}/mailboxes/${selectedMailboxId}/emails`);
            if (emails) {
                renderEmails(emails);
            }
        }
    });


    // --- Initial Load ---
    async function init() {
        const users = await apiFetch('/users');
        if (users) {
            renderUsers(users);
        } else {
            usersList.innerHTML = '<li>Failed to load users.</li>';
        }
        renderEmailDetail(null); // Show placeholder initially
    }

    init();
});
