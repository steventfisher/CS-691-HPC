# Very basic makefile.
# Declaring a variable. 
filename=steven_fisher_hw1

# The source directory
src=source

# Defining a variable that will hold a list of all the source
# tex files.
source_files=$(wildcard ${src}/*.tex)

# Derefencing the variable: ${filnema}.pdf expands to
# hw1cs666_aleternate.pdf
# target: prerequisites
#	recipe
# target is the thing to create, prerequisite are the things that need
# to be made first and, if they change, then recreate the
# target. Recipe is the thing to run to make the target. As part of
# the syntax, recipe 'must' be preceeded by a tab.
${filename}.pdf: ${src}/${filename}.tex ${source_files}
	pdflatex ${filename}.tex

#p1v2: p1v2.c
#	gcc -Wall -std=gnu99 -o p1v2 p1v2.c -lm

.PHONY: clean

clean:
	rm -f ${filename}.log ${filename}.aux ${filename}.pdf
