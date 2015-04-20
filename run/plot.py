#!/usr/bin/env python

import csv
import sys
import matplotlib.pyplot as plt

f = open(sys.argv[1], 'rb')
reader = csv.DictReader(f)
la = []
ld = []
lc = []
ls = []
lm = []
lx = []
for line in reader:
	for lst,e,a in [	[la,'Avegare (Estimated)','Avegare (Actual)'],
						[ld,'Stand. Deviation (Estimated)','Stand. Deviation (Actual)'],
						[lc,'Count (Estimated)','Count (Actual)'],
						[ls,'Sum (Estimated)','Sum (Actual)'],
						[lm, 'Min (Estimated)','Min (Actual)'],
						[lx, 'Max (Estimated)','Max (Actual)'],
					]:
						
		if float(line[a]) == 0.0:
			lst.append(0)
		else:
			lst.append(abs(float(line[a])-float(line[e]))/float(line[a])*100)

plt.plot(la,label="average")
plt.plot(ld,label='stddev')
plt.plot(lc,label='count')
plt.plot(ls,label='sum')
plt.plot(lm,label='min')
plt.plot(lx,label='max')
plt.title(sys.argv[1])
plt.ylabel("% Deviation of Estimate vs Actual")
plt.xlabel("Epoch")
plt.legend()
plt.show()
		
