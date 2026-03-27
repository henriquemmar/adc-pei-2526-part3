package pt.unl.fct.di.adc.firstwebapp.util;

public class ResponseWrapper<T> {
    public String status;
    public T data;

    public ResponseWrapper(T data) {
        this.status = "success";
        this.data = data;
    }
}
