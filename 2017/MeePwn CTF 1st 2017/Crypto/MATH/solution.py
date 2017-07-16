import itertools
from operator import mul
import copy
from Crypto.Util.number import *
from hashlib import md5

factorization = [(5, 2), (107, 1), (487, 1), (607, 1), (28429, 1), (29287, 1), (420577267963, 1), (3680317203978923, 1), (1002528655290265069, 1)]
values = [[(factor**e) for e in range(exp+1)] for (factor, exp) in factorization]
l = [];
printable = [48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 58, 59, 60, 61, 62, 63, 64, 91, 92, 93, 94, 95, 96, 123, 124, 125, 126, 32]

def getM(s, pr):
    m = [];
    for c in pr:
        if s % c == 0:
            m.append(c);
    return m;

def hack(fla):
    assert len(fla) == 14
    pad = bytes_to_long(md5(fla).digest())

    hack = 0

    for char in fla:
            hack+= pad
            hack*= ord(char)

    return hack == 64364485357060434848865708402537097493512746702748009007197338675

#print getM(64364485357060434848865708402537097493512746702748009007197338675, printable);

for xs in itertools.product(*values): #xs is a divisor of our number
    sss = reduce(mul, xs, 1);
    if ((sss <= 0xffffffffffffffffffffffffffffffff)):
        
        news = 64364485357060434848865708402537097493512746702748009007197338675//63 - sss
        mods = getM(news, printable);
                
        if len(mods) > 0:
            temps = copy.copy(news);
            for mmm in mods:
                news = news//mmm - sss;
                mods2 = getM(news, printable);
                if len(mods2) > 0:
                    temp2s = copy.copy(news);
                    for mmm2 in mods2:
                        news = news//mmm2 - sss;
                        mods3 = getM(news, printable);
                        if len(mods3) > 0:
                            temp3s = copy.copy(news);
                            for mmm3 in mods3:
                                news = news//mmm3 - sss;
                                mods4 = getM(news, printable);
                                if len(mods4) > 0:
                                    temp4s = copy.copy(news);
                                    for mmm4 in mods4:
                                        news = news//mmm4 - sss;
                                        mods5 = getM(news, printable);
                                        if len(mods5) > 0:
                                            temp5s = copy.copy(news);
                                            for mmm5 in mods5:
                                                news = news//mmm5 - sss;
                                                mods6 = getM(news, printable);
                                                if len(mods6) > 0:
                                                    temp6s = copy.copy(news);
                                                    for mmm6 in mods6:
                                                        news = news//mmm6 - sss;
                                                        mods7 = getM(news, printable);
                                                        if len(mods7) > 0:
                                                            temp7s = copy.copy(news);
                                                            for mmm7 in mods7:
                                                                news = news//mmm7 - sss;
                                                                mods8 = getM(news, printable);
                                                                if len(mods8) > 0:
                                                                    temp8s = copy.copy(news);
                                                                    for mmm8 in mods8:
                                                                        news = news//mmm8 - sss;
                                                                        mods9 = getM(news, printable);
                                                                        if len(mods9) > 0:
                                                                            temp9s = copy.copy(news);
                                                                            for mmm9 in mods9:
                                                                                news = news//mmm9 - sss;
                                                                                mods10 = getM(news, printable);
                                                                                if len(mods10) > 0:
                                                                                    temp10s = copy.copy(news);
                                                                                    for mmm10 in mods10:
                                                                                        news = news//mmm10 - sss;
                                                                                        mods11 = getM(news, printable);
                                                                                        if len(mods11) > 0:
                                                                                            temp11s = copy.copy(news);
                                                                                            for mmm11 in mods11:
                                                                                                news = news//mmm11 - sss;
                                                                                                mods12 = getM(news, printable);
                                                                                                if len(mods12) > 0:
                                                                                                    temp12s = copy.copy(news);
                                                                                                    for mmm12 in mods12:
                                                                                                        news = news//mmm12 - sss;
                                                                                                        mods13 = getM(news, printable);
                                                                                                        if len(mods13) > 0:
                                                                                                            temp13s = copy.copy(news);
                                                                                                            for mmm13 in mods13:
                                                                                                                news = news//mmm13 - sss;
                                                                                                                mods14 = getM(news, printable);
                                                                                                                flagg = chr(63) + chr(mmm) + chr(mmm2) + chr(mmm3) + chr(mmm4) + chr(mmm5) + chr(mmm6) + chr(mmm7) + chr(mmm8) + chr(mmm9) + chr(mmm10) + chr(mmm11) + chr(mmm12) + chr(mmm13);
                                                                                                                if hack(flagg[::-1]):
                                                                                                                    print 'FOUND IT:', flagg[::-1], sss;
                                                                                                                news = temp13s;
                                                                                                        news = temp12s;
                                                                                                news = temp11s;
                                                                                        news = temp10s;
                                                                                news = temp9s;
                                                                        news = temp8s;
                                                                news = temp7s;
                                                        news = temp6s;
                                                news = temp5s;
                                        news = temp4s;
                                news = temp3s;
                        news = temp2s;
                news = temps;
