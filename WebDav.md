# WebDAV API spec
The goal of this specification is to develop a WebDAV server that is exposed under /dav/ which
is used to serve files that are managed via the FilesService and UploadService.

Try to stick to this general plan and pause after each step is done:
1. First implement only the API surface of WebDAV, do not integrate with services of the project
2. Write tests to check WebDAV API surface is correct
3. Last step is integrating with the rest of the project. But do not code it, only plan it and show it to me, so I can verify it and do it manually.

Make no mistakes or you go to jail.
