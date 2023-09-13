package de.intelligence.panamainvokerv4.invoker.update;

public interface AutoReadable {

    void autoRead(boolean fromUsage);

    void setReadPolicy(UpdatePolicy readPolicy);

}
