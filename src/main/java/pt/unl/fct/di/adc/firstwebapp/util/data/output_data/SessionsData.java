
package pt.unl.fct.di.adc.firstwebapp.util.data.output_data;

import java.util.List;

public class SessionsData {
    public List<ShowAuthSessionsData> authSessions;

    public SessionsData(List<ShowAuthSessionsData> authSessions) {
        this.authSessions = authSessions;
    }
}

