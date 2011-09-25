/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.laytonsmith.aliasengine.functions;

import com.laytonsmith.aliasengine.Constructs.CArray;
import com.laytonsmith.aliasengine.Constructs.Construct;
import com.laytonsmith.aliasengine.functions.Exceptions.ExceptionType;
import com.laytonsmith.aliasengine.functions.exceptions.ConfigRuntimeException;
import java.io.File;
import java.util.regex.Pattern;
import org.bukkit.command.CommandSender;

/**
 *
 * @author Layton
 */
public class Regex {
    
    public static String docs(){
        return "This class provides regular expression functions. For more details, please see the page on "
                + "[[CommandHelper/Regex|regular expressions]]";
    }
    
    @api public static class reg_match implements Function{

        public String getName() {
            return "reg_match";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "array {pattern, subject} Searches for the given pattern, and returns an array with the results. Captures are supported."
                    + " If the pattern is not found anywhere in the subject, an empty array is returned. The indexes of the array"
                    + " follow typical regex fashion; the 0th element is the whole match, and 1-n are the captures specified in"
                    + " the regex.";
        }

        public ExceptionType[] thrown() {
            return new ExceptionType[]{};
        }

        public boolean isRestricted() {
            return false;
        }

        public void varList(IVariableList varList) {}

        public boolean preResolveVariables() {
            return true;
        }

        public String since() {
            return "3.2.0";
        }

        public Boolean runAsync() {
            return null;
        }

        public Construct exec(int line_num, File f, CommandSender p, Construct... args) throws ConfigRuntimeException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    @api public static class reg_replace implements Function{

        public String getName() {
            return "reg_replace";
        }

        public Integer[] numArgs() {
            return new Integer[]{3};
        }

        public String docs() {
            return "string {pattern, replacement, subject} Replaces any occurances of pattern with the replacement in subject."
                    + " Back references are allowed.";
        }

        public ExceptionType[] thrown() {
            return new ExceptionType[]{};
        }

        public boolean isRestricted() {
            return false;
        }

        public void varList(IVariableList varList) {}

        public boolean preResolveVariables() {
            return true;
        }

        public String since() {
            return "3.2.0";
        }

        public Boolean runAsync() {
            return null;
        }

        public Construct exec(int line_num, File f, CommandSender p, Construct... args) throws ConfigRuntimeException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    @api public static class reg_split implements Function{

        public String getName() {
            return "reg_split";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "array {pattern, subject}";
        }

        public ExceptionType[] thrown() {
            return new ExceptionType[]{};
        }

        public boolean isRestricted() {
            return false;
        }

        public void varList(IVariableList varList) {}

        public boolean preResolveVariables() {
            return true;
        }

        public String since() {
            return "3.2.0";
        }

        public Boolean runAsync() {
            return null;
        }

        public Construct exec(int line_num, File f, CommandSender p, Construct... args) throws ConfigRuntimeException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }  
    
    @api public static class reg_count implements Function{

        public String getName() {
            return "reg_count";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "int {pattern, subject}";
        }

        public ExceptionType[] thrown() {
            return new ExceptionType[]{};
        }

        public boolean isRestricted() {
            return false;
        }

        public void varList(IVariableList varList) {}

        public boolean preResolveVariables() {
            return true;
        }

        public String since() {
            return "3.2.0";
        }

        public Boolean runAsync() {
            return null;
        }

        public Construct exec(int line_num, File f, CommandSender p, Construct... args) throws ConfigRuntimeException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    private static Pattern getPattern(Construct c, int line_num, File f){
        String regex = "";
        int flags = 0;
        String sflags = "";
        if(c instanceof CArray){
            CArray ca = (CArray)c;
            regex = ca.get(0, line_num).val();
            sflags = ca.get(1, line_num).val();
            for(int i = 0; i < sflags.length(); i++){
                if(sflags.toLowerCase().charAt(i) == 'i'){
                    flags |= Pattern.CASE_INSENSITIVE;
                } else if(sflags.toLowerCase().charAt(i) == 'm'){
                    flags |= Pattern.MULTILINE;
                } else if(sflags.toLowerCase().charAt(i) == 's'){
                    flags |= Pattern.DOTALL;
                } else {
                    throw new ConfigRuntimeException("Unrecognized flag: " + sflags.toLowerCase().charAt(i), ExceptionType.FormatException, line_num, f);
                }
            }
        } else {
            regex = c.val();
        }
        return Pattern.compile(regex, flags);
    }
}
