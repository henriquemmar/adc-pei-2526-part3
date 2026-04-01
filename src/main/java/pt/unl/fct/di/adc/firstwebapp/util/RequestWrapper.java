package pt.unl.fct.di.adc.firstwebapp.util;

import pt.unl.fct.di.adc.firstwebapp.util.data.output_data.AuthToken;

public class RequestWrapper<T> {
    public T input;
    public AuthToken token;
    public boolean isInputValid() {
        return input != null;
    }
    public boolean isTokenValid(){
        return token != null;
    }
}
