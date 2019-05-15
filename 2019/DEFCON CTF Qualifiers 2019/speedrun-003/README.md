# speedrun-003 (speedrun, shellcoding, pwnable)

**DISCLAIMER**: Solved after competition ended.

## Recon

Lets look at our binary and see what we are dealing with

```
vagrant@kali:/vagrant/misc$ file speedrun-003
speedrun-003: ELF 64-bit LSB shared object, x86-64, version 1 (SYSV), dynamically linked, interpreter /lib64/ld-linux-x86-64.so.2, for GNU/Linux 3.2.0, BuildID[sha1]=6169e4b9b9e1600c79683474c0488c8319fc90cb, not stripped

vagrant@kali:/vagrant/misc$ ./speedrun-003
Think you can drift?
Send me your drift
asdfsdfafdasadfsfdsfds
You're not ready.

```

Lets look at the disassembly using IDA Pro and see what exactly the binay is doing:

```
__int64 get_that_shellcode()
{
  int v0; // ST0C_4@1
  char v1; // ST0A_1@7
  char buf; // [sp+10h] [bp-30h]@1
  char v4; // [sp+1Fh] [bp-21h]@7
  char v5; // [sp+2Eh] [bp-12h]@1
  __int64 v6; // [sp+38h] [bp-8h]@1

  v6 = *MK_FP(__FS__, 40LL);
  puts("Send me your drift");
  v0 = read(0, &buf, 0x1EuLL);
  v5 = 0;
  if ( v0 == 30 )
  {
    if ( strlen(&buf) == 30 )
    {
      if ( strchr(&buf, 0x90) )
      {
        puts("Sleeping on the job, you're not ready.");
      }
      else
      {
        v1 = xor((__int64)&buf, 15u);
        if ( v1 == (unsigned __int8)xor((__int64)&v4, 15u) )
          shellcode_it(&buf, 0x1Eu);
        else
          puts("This is a special race, come back with better.");
      }
    }
    else
    {
      puts("You're not up to regulation.");
    }
  }
  else
  {
    puts("You're not ready.");
  }
  return *MK_FP(__FS__, 40LL) ^ v6;
}
```
This is a shellcode challenge, so the program wants us to craft shellcode that is exactly 30 bytes.

Then it checks if we didn't have any NOP instructions (0x90) in our shellcode.

Finally, it calls a function xor() and checks an equality.

If that equality returns true, then we call shellcode_it(). Shellcode_it() runs our shellcode by creating a memory map using mmap(), copying our shellcode to that address space and executing it.

If you want a cleaner and simpler example, check out this [gist](https://gist.github.com/CoolerVoid/1557916).

Lets look at the function xor():

```
__int64 __fastcall xor(__int64 a1, unsigned int a2)
{
  unsigned __int8 v3; // [sp+17h] [bp-5h]@1
  unsigned int i; // [sp+18h] [bp-4h]@1

  v3 = 0;
  for ( i = 0; i < a2; ++i )
    v3 ^= *(_BYTE *)(i + a1);
  return v3;
}
```

So this function takes a buffer indicated by a1, and xors each byte starting from the beginning until buffer + a2, and returns the result.

Lets see how xor is used:

```
v1 = xor((__int64)&buf, 15u);
if ( v1 == (unsigned __int8)xor((__int64)&v4, 15u) )
	shellcode_it(&buf, 0x1Eu);
```

and 

```
char buf; // [sp+10h] [bp-30h]@1
char v4; // [sp+1Fh] [bp-21h]@7
```

So, the first xor call xors the first `15` bytes of our buffer together, and stores that result in `v1`.

Then, we compare `v1` to the result of xoring the last `15` bytes of our buffer together (because `0x1F - 0x10 = 15`).

So essentially in our shellcode we need the first `15` bytes xor'd together to equal the last `15` bytes xor'd together.


