# Challenge
Chunk Norris is black belt in fast random number generation.

We are given 2 files, a `challenge.py` and `output.txt` file. It looks like the flag was encrypted with textbook RSA, and that the 2 primes were the modulus (both 1024-bit) were generated in an unsafe way.

The goal is to recover the primes and decrypt the ciphertext to get the flag.
