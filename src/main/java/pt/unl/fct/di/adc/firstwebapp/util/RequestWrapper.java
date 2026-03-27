package pt.unl.fct.di.adc.firstwebapp.util;

public class RequestWrapper<T> {
    public T input;
    public String token;
    public boolean isInputValid() {
        return input != null;
    }
    public boolean isTokenValid(){
        return token != null && !token.isBlank();
    }
}
