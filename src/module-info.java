module org.oxoo2a.sim4da {
    requires java.base;
    requires java.logging;
    requires org.slf4j;

    exports org.oxoo2a.sim4da;
    opens org.oxoo2a.sim4da to java.base;
}