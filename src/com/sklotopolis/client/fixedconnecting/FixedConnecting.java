package com.sklotopolis.client.fixedconnecting;

import javassist.*;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class FixedConnecting implements WurmClientMod, PreInitable {


    private int counter = 0;
    @Override
    public void init() {



    }

    @Override
    public void preInit() {

        try {

            ClassPool classpool = HookManager.getInstance().getClassPool();

            CtClass baseClientClass = classpool.get("com.wurmonline.client.WurmClientBase");
            CtField fieldseconds = new CtField(CtClass.intType, "seconds", baseClientClass);
            baseClientClass.addField(fieldseconds,"1");


            classpool.get("com.wurmonline.client.WurmClientBase").getDeclaredMethod("performConnection").instrument(new ExprEditor() {

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals("com.wurmonline.client.WurmClientBase") && m.getMethodName().equals("startupMessage")) {

                        if(counter==0) {
                            m.replace("" +
                                    "$1 = \"Sending Steam Authentication...\";" +
                                    "$_ = $proceed($$);");
                            counter++;
                        }
                        else if(counter==1)
                        {
                            m.replace("" +

                                    "if(j==29)" +
                                    "{" +
                                    "   com.wurmonline.client.WurmClientBase.logger.log(java.util.logging.Level.INFO, \"Waiting for server\");" +
                                    "}" +
                                    "if(j==10){this.seconds++;}" +


                                    "$1 = \"Connecting... Please wait about 20 seconds, reconnect if nothing happened. \"+this.seconds+\" seconds passed.\";" +
                                    "$_ = $proceed($$);");
                            counter++;
                        }

                    }

                }
            });


            classpool.get("com.wurmonline.client.comm.SimpleServerConnectionClass").getDeclaredMethod("sendSteamAuthTicket").insertBefore(

                    "com.wurmonline.client.comm.SimpleServerConnectionClass.logger.log(java.util.logging.Level.INFO, \"Steam AUTH Sending\");"

            );


            classpool.get("com.wurmonline.client.comm.SimpleServerConnectionClass").getDeclaredMethod("reconn").insertBefore(


                    "com.wurmonline.client.comm.SimpleServerConnectionClass.logger.log(java.util.logging.Level.INFO, \"Forwarding to new server. reconn sessionkey: \"+$3);"

            );

            classpool.get("com.wurmonline.client.comm.SimpleServerConnectionClass").getDeclaredMethod("sendReconn").insertBefore(

                    "com.wurmonline.client.comm.SimpleServerConnectionClass.logger.log(java.util.logging.Level.INFO, \"Steam Auth OK on new server, sendReconn.\");"

            );

            CtClass connectionClass = classpool.get("com.wurmonline.client.comm.SimpleServerConnectionClass");
            CtField field = new CtField(CtClass.intType, "modCounter", connectionClass);
            connectionClass.addField(field,"0");

            classpool.get("com.wurmonline.client.comm.SimpleServerConnectionClass").getDeclaredMethod("checkAuthenticationShouldReconnect").insertBefore("" +
                    "" +
                    "this.modCounter++;" +
                    "if(this.modCounter>188)" +
                    "{" +
                    "   this.modCounter=0;" +
                    "   com.wurmonline.client.WurmClientBase.steamHandler.cancelAuthTicket();" +
                    "   this.sendSteamAuthTicket(true);" +
                            "com.wurmonline.client.comm.SimpleServerConnectionClass.logger.log(java.util.logging.Level.INFO, \"Server didn't respond to steam auth, retrying...\");"+
                    "}");




        } catch (NotFoundException | CannotCompileException e) {
            throw new HookException(e);
        }


    }
}
