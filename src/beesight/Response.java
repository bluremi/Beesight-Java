/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package beesight;

import java.util.ArrayList;
/**
 * 
 * @author Phil Brosgol <phil.brosgol at gmail.com>
 */
public class Response {
    private ArrayList<Session> sessions = new ArrayList<>();
    
    public ArrayList<Session> getSessions(){
        return sessions;
    }
}
