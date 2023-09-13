package de.intelligence.panamainvokerv4.invoker.update;

public interface AutoWritable {

    void autoWrite(boolean fromUsage);

    void setWritePolicy(UpdatePolicy writePolicy);

}
