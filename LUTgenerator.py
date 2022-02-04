import numpy as np
import math

count = 0

OFFSET = 50

MODD = 4

DEBUG = False
ansDEBUG = []
iDEBUG = []
jDEBUG = []

if not DEBUG: print("when (X1) {")

d_uint = lambda x: "u(\"9'd" + str(x) + "\")"
b_uint = lambda x: "u(\"32'b" + str(x) + "\")"

for i in range(OFFSET, 320-OFFSET):
  if (i%MODD==0):
    if not DEBUG: print("\t", d_uint(i//4), "-> { outputdistance = when (X2) {")
    for j in range(i, 320-OFFSET):
      if (j%MODD==0):
        count += 1

        ans = f(j, i)
        if DEBUG:
          print(ans, i, j)
          ansDEBUG.append(ans)
          iDEBUG.append(i)
          jDEBUG.append(j)

        if str(ans) == "inf": ans = -10000
        if ans > 9999: ans = 9999
        if ans < 0:
          ansarray = ["1111"]*8#ans = 2**32 - 1
        else:
          ansarray = list( ('%.4f' % ans).zfill(9).replace(".", "") )

          ansarray = [str("{0:04b}".format(int(i))) for i in ansarray]

        if not DEBUG: print("\t\t", d_uint(j//MODD), "->", b_uint("".join(ansarray)), "//", ans)

    if not DEBUG: print("\t\t else -> u0() } }")

if not DEBUG: print(" else -> {} }")