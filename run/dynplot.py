import time
import numpy as np
import matplotlib.pyplot as plt
import subprocess
import sys
import csv

folder=sys.argv[1]
plt.title(folder)
plt.ylabel("% Deviation of Estimate vs Actual")
plt.xlabel("Epoch")
plt.ylim([0,200])
plt.xlim([0,200])

l_avg, = plt.plot([],label="average")
l_std, = plt.plot([],label='stddev')
l_cnt, = plt.plot([],label='count')
l_sum, = plt.plot([],label='sum')
l_min, = plt.plot([],label='min')
l_max, = plt.plot([],label='max')

plt.legend()
plt.ion()
plt.show()

while True:
	print "Dump"
	subprocess.call("java -cp bin/lib/*:bin/classes/ protocols.DIASLogReplayer dump/%s | tail -n+4 > summaries/%s.dat"%(folder,folder),shell=True)
	print "Read"
	f = open("summaries/%s.dat"%folder, 'rb')
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

	print "Update"
	l_avg.set_data(range(len(la)),la)
	l_std.set_data(range(len(ld)),ld)
	l_cnt.set_data(range(len(lc)),lc)
	l_sum.set_data(range(len(ls)),ls)
	l_min.set_data(range(len(lm)),lm)
	l_max.set_data(range(len(lx)),lx)
	plt.draw()

	print "Wait"
	time.sleep(10)