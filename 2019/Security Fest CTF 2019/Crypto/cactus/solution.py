import binascii

def solve(o):
    st = []
    for i in range(11):
        ss_hex = hex(o >> i)[2:].replace('L', '')
        if len(ss_hex) % 2 == 1:
            ss_hex = '0' + ss_hex
        st.append(binascii.unhexlify(ss_hex))
    return st

outputs = open("output.txt", "rb").readlines()
for o in outputs:
    w = solve(long(o))
    for i in range(11):
        if "sctf" in w[i]:
            print w[i]
