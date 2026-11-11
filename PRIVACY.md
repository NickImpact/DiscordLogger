# Gister Privacy Policy

**Effective Date:** November 10, 2026  
**Application:** Gister  
**Developer:** NickImpact  
**Source Code:** https://github.com/NickImpact/DiscordLogger

## 1. Introduction

Gister ("the Application," "we," "our," or "us") is a Discord application designed to improve attachment accessibility by allowing supported files shared within Discord servers to be reuploaded and accessed through a bot-generated response.

The service is primarily intended to assist mobile users and individuals who may encounter limitations when viewing or downloading certain attachments through Discord's desktop or mobile applications.

Gister acts as an intermediary between Discord and the third-party file-hosting service mclo.gs. The Application does not operate as a message-logging service and does not maintain an independent archive of Discord conversations.

This Privacy Policy explains what information Gister processes, how attachments are handled, where files are hosted, and what control users have over stored files.

## 2. Information We Process

Gister is designed to minimize the collection and retention of user information.

The Application does not independently retain Discord message contents, conversation histories, usernames, or other personal Discord information in a database.

Information temporarily handled by Gister is limited to what is necessary to facilitate attachment processing and file deletion.

This may include:

- **Attachment information:** File names, file extensions, attachment URLs, and upload timestamps, as necessary to identify and process eligible attachments.
- **Hosting information:** The unique mclo.gs identifier and associated deletion token required to identify and delete an uploaded file.
- **Discord component information:** The deletion credentials associated with a bot-generated response, embedded within its interactive button component to facilitate user-requested deletion.

The deletion credentials are not displayed as ordinary text within the Discord message. However, they are included in the interactive component's underlying data and may be accessible through Discord's interaction infrastructure.

Gister does not independently maintain a database of Discord conversations, user profiles, or attachment histories.

## 3. Attachment Filtering and Processing

Gister does not indiscriminately download or reupload every attachment shared within a Discord server.

The Application uses an extension-validation mechanism within its `LoggingListener` class to determine whether an attachment is eligible for processing.

Only file extensions explicitly identified as valid by the Application's `valid` method will be considered for reupload.

**All attachments that do not meet these extension requirements are ignored.** Unsupported files are not intentionally downloaded, stored, or reuploaded by Gister.

Eligible attachments are retrieved and forwarded to the mclo.gs API for hosting.

The purpose of this process is solely to improve attachment accessibility for Discord users.

Gister does not use the contents of uploaded attachments for analytics, advertising, profiling, or unrelated purposes.

## 4. Third-Party File Hosting: mclo.gs

Gister utilizes [mclo.gs](https://mclo.gs/) as its third-party file-hosting provider.

mclo.gs is a service primarily designed for hosting and sharing Minecraft logs. Its functionality includes automatically identifying and concealing sensitive information, such as users' IP addresses, within supported log content.

Attachments processed by Gister are uploaded to mclo.gs rather than being maintained in an independent file-hosting database operated by Gister.

Consequently:

- Files are subject to the hosting and data-handling practices of mclo.gs.
- mclo.gs may independently process uploaded content as necessary to provide its hosting and sanitization functionality.
- Gister relies on the mclo.gs API to identify and delete hosted files.
- Users should review the policies and practices of mclo.gs before submitting sensitive information.

For additional information about the hosting provider, visit:

https://mclo.gs/

Gister is an independent application and is not affiliated with, endorsed by, or officially sponsored by mclo.gs.

## 5. File Retention and Automatic Deletion

Files uploaded through Gister are hosted by mclo.gs, which guarantees automatic expiration of hosted files after three (3) months from the last view, according to its service behavior at the time this policy was published.

The retention policy is as follows:

- **Retention period:** Three (3) months.
- **Expiration calculation:** The retention period begins from the latest view of the file.
- **Automatic deletion:** mclo.gs automatically expires hosted files after the three-month retention period.
- **Early deletion:** Users who initiate a Gister bot response are provided with an option to request early deletion of the associated files.

The three-month retention period is intended to ensure that hosted attachments do not remain available indefinitely.

Gister uses the mclo.gs file identifier and associated deletion token to facilitate removal of uploaded files before their automatic expiration.

Once a file has been successfully deleted from mclo.gs, it will no longer be accessible through its hosted link.

Please note that deletion from mclo.gs does not necessarily remove copies previously downloaded, independently saved, cached, or redistributed by other users or third-party services.

The expiration guarantee described in this section reflects mclo.gs's service behavior at the time of publication and remains subject to changes in that provider's service and policies.

## 6. User-Controlled Deletion and Credential Recovery

Gister provides users with the ability to request premature deletion of files associated with their bot-generated responses.

The user who initiated the original bot response is provided with an interactive deletion option. This option uses the associated mclo.gs identifier and deletion token to identify and remove the relevant hosted file.

The deletion credentials are embedded within the interactive Discord component rather than being displayed as ordinary message text.

### Recovery of Deletion Credentials

If a server moderator or another authorized user deletes the Discord message containing the interactive deletion component, the credentials associated with that component may otherwise become inaccessible.

To prevent users from losing control over their hosted files in these circumstances, Gister also provides the original user with a private message containing the mclo.gs identifier and deletion token associated with their uploaded files.

This allows users to retain the information necessary to request file deletion even if the original bot-generated Discord message is removed.

The credentials are intended solely for managing and deleting the associated hosted files. Users should treat deletion tokens as private information and avoid sharing them publicly.

Please be aware that this feature requires that users have the "Direct Messages from server members" option enabled. If this has been disabled, this message will fail to be sent and users will need to make use of the actual message to delete their files prematurely.

## 7. Information Sharing and Disclosure

Gister does not sell, rent, or distribute user information or uploaded attachments for advertising, marketing, or commercial data-brokering purposes.

Information is transmitted to third-party services only as necessary to provide the Application's functionality.

This includes:

- **Discord:** For receiving attachment information, delivering bot-generated responses, and transmitting interactive component data.
- **mclo.gs:** For hosting, processing, and deleting eligible attachments.

Gister does not independently share Discord conversation histories or maintain user profiles.

Third-party service providers operate under their own privacy policies and data-handling practices. Gister cannot guarantee how information is handled outside of its own application.

Information may also be disclosed where required to comply with applicable laws, regulations, or valid legal requests.

## 8. Data Security

Gister is designed to minimize the amount of information it independently retains.

The Application relies on the security measures and infrastructure of Discord and mclo.gs when processing and hosting attachments.

Reasonable measures are taken within Gister to prevent unauthorized use of the information necessary to manage hosted files.

However, no internet-based service or electronic storage system can guarantee absolute security.

Users should avoid submitting sensitive, confidential, or personally identifiable documents through Gister unless they are comfortable with the temporary hosting and processing described in this policy.

## 9. Discord and Third-Party Services

Gister operates through the Discord platform and interacts with Discord's APIs and infrastructure.

Your use of Discord remains subject to Discord's own Terms of Service and Privacy Policy.

Likewise, files uploaded through Gister are subject to the applicable terms and privacy practices of mclo.gs.

Gister is an independent application and is not affiliated with, endorsed by, or officially sponsored by Discord Inc. or mclo.gs.

## 10. Changes to This Privacy Policy

We reserve the right to modify or update this Privacy Policy as the Application's functionality, infrastructure, or legal requirements evolve.

Changes will be reflected in the published version of this document, with the effective date updated accordingly.

Users are encouraged to periodically review this policy to remain informed about how Gister handles information and hosted attachments.

## 11. Contact Information

For questions, concerns, or inquiries regarding this Privacy Policy, data handling, or attachment deletion, please contact the developer through the project's GitHub repository.

**Developer:** NickImpact

**GitHub Repository:** https://github.com/NickImpact/DiscordLogger

**Privacy Inquiries and Support:** https://github.com/NickImpact/DiscordLogger/issues

Please avoid including sensitive personal information, private attachment links, or deletion credentials in publicly accessible GitHub issues.