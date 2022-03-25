## R.a > 200 (clustered index scan)

Clustered index scan I/O operations = X * B(R)

X = (max(R.a) - 200) / (max(R.a) - min(R.a))
  = (250 - 200) / (250 - 150) = 50 / 100 = 1/2

B(R) = 1000

So, clustered index scan = 1/2 * 1000 = 500 I/O operations

## R.a = S.a (block-at-a-time nested loop join)

Block-at-a-time nested loop join = B(S) * B(R)

B(S) = 2000
B(R) = 500 (after the clustered index scan)

= 2000 * 500 = 1,000,000 I/O operations


## S.b = U.b (unclustered nested loop join)
Unclustered nested loop join = B(RS) + T(RS) * T(U) / V(U, b)

B(RS) = 0 since already loaded into memory
T(RS) = 4 * 10^8
T(U) = 10^4
V(U, b) = 250
= (4 * 10^8) * 10^4 / 250 = 16 billion

## Project R.a, S.c
I/O operations = 0

## Total cost
500 + 1,000,000 + 16 billion + 0 = 16,100,500 I/O operations