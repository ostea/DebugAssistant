#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <strings.h>
#include <errno.h>
#include <unistd.h>
#include <arpa/inet.h>
#include "vperf.h"

#define BUFSIZE 1024

//vperf host port data
int main(int argc, char **argv) {
    if (argc < 4) {
        printf("usage: vperf host port data\n");
        return -1;
    }
    char *host = argv[1];
    int port = atoi(argv[2]);
    int count = atoi(argv[3]);

    int sockfd;
    int serverlen;
    struct sockaddr_in serveraddr;
    char buf[BUFSIZE];

    bzero(&serveraddr, sizeof(serveraddr));
    serveraddr.sin_family = AF_INET;
    serveraddr.sin_addr.s_addr = inet_addr(host);
    serveraddr.sin_port = htons(port);

    /* socket: create the socket */
    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        printf("error when create socket\n");
        return -1;
    }

    /* init message data*/
    if (argc >= 5) {
        strcpy(buf, argv[4]);
    } else {
        strcpy(buf, "xxxxxxxx");
    }


    /* send the message to the server */
    printf("start...\n");
    serverlen = sizeof(serveraddr);
    for (int i = 0; i < count; i++) {
        sendto(sockfd, buf, strlen(buf), 0, (struct sockaddr *) &serveraddr, serverlen);
    }
    printf("end\n");
    return 0;
}