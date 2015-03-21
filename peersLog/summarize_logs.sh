find * -type d | while read i
do
	if [ ! -e "$i.summary" ]; then
		java -cp $(find ../dist/lib/*.jar| tr '\n' ':'):../build/classes/ protocols.DIASLogReplayer "$i" > "$i.summary"
		tail -n+4 $i.summary > t
		mv t $i.summary
	fi
	python plot.py $i.summary &
done
