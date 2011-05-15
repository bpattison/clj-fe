//#include <stdafx.h>
#include <stdio.h>
#include <stdlib.h>
#include <direct.h>
#include <winsock2.h>
#include <string.h>
#pragma comment(lib, "wsock32.lib")

static int xxx = _MSC_VER;

static char *server_address = "127.0.0.1";
static int   server_port = 4442;
static int   server_socket = 0;

static HANDLE stdin_handle = (HANDLE) NULL;
static HANDLE stdout_handle = (HANDLE) NULL;
static HANDLE stderr_handle = (HANDLE) NULL;

const int BUFSIZE = 2048;

void cleanExit(int ecode) 
{
  CancelIo(stdin);
  //WSACleanup();   // -- way too slow

  if (server_socket)
    closesocket(server_socket);

  exit(ecode);
}

int sendData(char *buf, int len)
{
  // fprintf(stdout, "sendData=[%s]\n", buf);

  char *data = buf;
  int   rem = len;
  int   n = 0;

  while(rem > 0) {
    n = send(server_socket, data, rem, 0);
    if (n == -1)
      break;
    data += n;
    rem -= n;
  }

  return (n == -1) ? 0 : len; 
}

int sendCmd(char *cmd, char *data)
{
  char lbuf[512]; // large buffer
  sprintf_s(lbuf, "[%d]", strlen(data));

  int abuflen = strlen(cmd) + strlen(lbuf) + strlen(data) + 1;
  char *abuf = (char *) malloc(abuflen);
  abuf[0] = 0;

  strcat_s(abuf, abuflen, cmd);
  strcat_s(abuf, abuflen, lbuf);
  strcat_s(abuf, abuflen, data);

  int r = sendData(abuf, abuflen);

  free(abuf);

  return r;
}

void handleError()
{
  int error = GetLastError();

  LPVOID lpMsgBuf;
  FormatMessage( FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |
    FORMAT_MESSAGE_IGNORE_INSERTS,
    NULL, error, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
    (LPTSTR) &lpMsgBuf, 0, NULL);

  // MessageBox( NULL, (LPCTSTR)lpMsgBuf, (LPCWSTR)"Error", MB_OK | MB_ICONERROR );
  fprintf(stderr, (const char *) lpMsgBuf);

  LocalFree( lpMsgBuf );

  cleanExit(error);
}

DWORD WINAPI processStdin(LPVOID) 
{
  char buf[BUFSIZE] = {0};

  while(1) {

    memset(buf,0,sizeof(buf));

    DWORD numbytes = 0;
    if (!ReadFile(stdin_handle, buf, sizeof(buf), &numbytes, NULL)) {
      if (numbytes != 0) {
        handleError();
      }
    }

    if (numbytes > 0) {
      char lbuf[512];
      sprintf_s(lbuf,sizeof(lbuf),"/in[%d]", numbytes);
      sendData(lbuf, strlen(lbuf));
      sendData(buf, numbytes);
    } else {
      sendData("/eof[0]", 7);
      break;
    }
  }

  return 0;
}

void recvToFD(HANDLE destFD, int len)
{
  char buf[BUFSIZE];
  int bytesRead = 0;

  while (bytesRead < len) {
    unsigned long bytesRemaining = len - bytesRead;
    int bytesToRead = (BUFSIZE < bytesRemaining) ? BUFSIZE : bytesRemaining;

    int rcount = recv(server_socket, buf, bytesToRead, 0);
    if (rcount < 0) {
      printf("recv failed: %d\n", WSAGetLastError());
      exit(-1);
    }

    bytesRead += rcount;

    int bytesCopied = 0;

    while(bytesCopied < rcount) 
    {
      DWORD wcount =  0;
      WriteFile(destFD, buf + bytesCopied,
                             rcount - bytesCopied, &wcount, NULL);

      if (wcount < 0)
        break;

      bytesCopied += wcount;
    }  
  }
}

int getLen()
{
  char digits[50] = {0};
  char b;

  if ((recv(server_socket, &b, 1, 0) != 1) || (b != '['))
    return 0;

  int i = 0;
  while ((recv(server_socket, &b, 1, 0) == 1) && (b >= '0' && b <= '9'))
    digits[i++] = b;

  if (b != ']')
    return 0;

  digits[i] = 0;

  int len = 0;
  sscanf_s(digits,"%d",&len);

  return len;
}

void usage(int exitcode)
{
  fprintf(stderr, "Usage: clj [<clj-file>] [args]\n");

  cleanExit(exitcode);
}

void initApp()
{
  WSADATA wsd;
  WSAStartup(2, &wsd);

  struct hostent *hostinfo = gethostbyname(server_address);

  if (hostinfo == NULL) {
    fprintf(stderr, "Unknown host: %s\n", server_address);
    cleanExit(-1);
  }

  if ((server_socket = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
    if ((server_socket = socket(AF_INET, SOCK_DGRAM, 0)) == -1) {
      perror("socket");
      cleanExit(-2);
    }
  }

  struct sockaddr_in server_addr;
  server_addr.sin_family = AF_INET;    
  server_addr.sin_port = htons(server_port);
  server_addr.sin_addr = *(struct in_addr *) hostinfo->h_addr;
  memset(&(server_addr.sin_zero), '\0', 8);

  if (connect(server_socket,
    (struct sockaddr *)&server_addr, sizeof(struct sockaddr)) == -1) {
      perror("connect");
      cleanExit(-3);
  }

  // non-block console io
  AllocConsole();

  stdin_handle  = GetStdHandle(STD_INPUT_HANDLE);
  stdout_handle = GetStdHandle(STD_OUTPUT_HANDLE);
  stderr_handle = GetStdHandle(STD_ERROR_HANDLE);

  SECURITY_ATTRIBUTES attrs;
  attrs.bInheritHandle = TRUE;
  attrs.lpSecurityDescriptor = NULL;
  attrs.nLength = 0;

  DWORD id = 0;
  if (!CreateThread(&attrs, 0, &processStdin, NULL, 0, &id))
    handleError();
}

int main(int argc, char *argv[], char *env[]) 
{
  initApp();

  // send arguments
  for (int i = 2; i < argc; i++)
    if (argv[i] != NULL)
      sendCmd("/arg", argv[i]);

  // send environment
  for (int i = 0; env[i]; i++)
    if (env[i] != NULL)
      sendCmd("/env", env[i]);

  // current working directory
  char *cwd = _getcwd(NULL, 0);
  sendCmd("/dir", cwd);
  free(cwd);

  // send clj file
  if (argc > 1) {
    sendCmd("/main", argv[1]);
  } else {
    fprintf(stderr, "Starting Clojure REPL...\nType Ctrl-Z <Enter> to exit\n");
    sendCmd("/main", "");
  }

  bool eof = false;

  // process input from stdin and from the server
  while(1) {
    char buf[5] = {0};
    int len = sizeof(buf)-1;
    if (recv(server_socket, buf, len, 0) < len)
      cleanExit(-4);
    if (strcmp(buf, "/out") == 0) {
      recvToFD(stdout_handle, getLen());
    } else if (strcmp(buf, "/err") == 0) {
      recvToFD(stderr_handle, getLen());
    } else if (strcmp(buf, "/ext") == 0) {
      // processExit(buf, getCmdLen());
    } else {
      fprintf(stderr, "Unexpected server command %s\n", buf);
      char dbuf[21] = {0};
      recv(server_socket, dbuf, sizeof(dbuf)-1, 0);
      cleanExit(-5);
    }
  }
}