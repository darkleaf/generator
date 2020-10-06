package darkleaf.generator;

import java.util.function.Supplier;
import clojure.lang.*;
import darkleaf.generator.proto.Generator;

public class LoomGenerator extends Continuation implements Generator {
    private static final ContinuationScope SCOPE = new ContinuationScope("darkleaf.generator");
    private static final Error INTERRUPTED_EXCEPTION = new Error("Interrupted generator");

    private Object value;
    private Object covalue;
    private Throwable coerror;
    private Object returnValue;

    public static Object yield(Object value) throws Throwable {
        LoomGenerator gen = (LoomGenerator)Continuation.getCurrentContinuation(SCOPE);
        //if (gen == null) {}
        gen.value = value;
        Continuation.yield(SCOPE);
        if (gen.coerror != null) {
            Throwable error = gen.coerror;
            gen.coerror = null;
            throw error;
        }
        return gen.covalue;
    }

    public LoomGenerator(Supplier body) {
        super(SCOPE, new Runnable() {
                public void run() {
                    LoomGenerator gen = (LoomGenerator)Continuation.getCurrentContinuation(SCOPE);
                    try {
                        gen.value = body.get();
                    } catch (Error ex) {
                        if (ex == INTERRUPTED_EXCEPTION) {
                            gen.value = gen.returnValue;
                        } else {
                            throw ex;
                        }
                    }
                }
            });
        this.run();
    }

    public Object _done_QMARK_() {
        return this.isDone();
    }

    public Object _value() {
        return this.value;
    }

    public Object _next(Object covalue) {
        this.rejectDone();
        this.covalue = covalue;
        this.run();
        return null;
    }

    public Object _throw(Object throwable) {
        this.rejectDone();
        this.coerror = (Throwable)throwable;
        this.run();
        return null;
    }

    public Object _return(Object returnValue) {
        this.rejectDone();
        this.returnValue = returnValue;
        this.coerror = INTERRUPTED_EXCEPTION;
        this.run();
        return null;
    }

    private void rejectDone() {
        if (this.isDone()) {
            IPersistentMap data = PersistentArrayMap.createAsIfByAssoc(new Object[] {
                    Keyword.intern("type"), Keyword.intern("illegal-state")
                });
            throw new ExceptionInfo("Generator is done", data);
        }
    }
}
