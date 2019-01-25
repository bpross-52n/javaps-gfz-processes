# wps.des: test.echo, title = dummy echo process, abstract = you get what you put in;
import sys
inputVariable=sys.argv[1]

# wps.in: id = inputVariable, type = string, title = input variable, minOccurs = 1, maxOccurs = 1;

print("Hello, " + inputVariable)

# wps.out: id = outputVariable, type = string, title = output variable;