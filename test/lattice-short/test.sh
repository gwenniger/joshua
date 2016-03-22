#!/bin/sh

# To get the US sort order on systems with a different locale, it is nesscessary to set LC_ALL, see sort man page:
#  *** WARNING *** The locale specified by the environment affects sort order.  Set LC_ALL=C to get the traditional sort order that uses native byte values.
export LC_ALL=C

cat input | $JOSHUA/bin/joshua-decoder -m 500m -config joshua.config 2> log | sort > output

if [[ $? -ne 0 ]]; then
	exit 1
fi

diff -u output output.expected > diff

if [[ $? -eq 0 ]]; then
  rm -f output log diff
  exit 0
else
  exit 1
fi
