JFLAGS = -cp
JC = javac
.SUFFIXES: .java .class
.java.class:
        $(JC) $(JFLAGS) . ctf/agent/$*.java

CLASSES = \
        ars140330.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
        $(RM) *.class