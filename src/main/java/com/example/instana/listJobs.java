package com.example.instana;

///////////////////////////////////////////////////////////////////////////
//
// This program is an example of the "job" classes in the
// IBM Toolbox for Java.  It gets a list of jobs on the server
// and outputs the job's status followed by job identifier.
//
//
// Command syntax:
//    listJobs system userID password
//
// (UserID and password are optional)
//
/////////////////////////////////////////////////////////////////////////

import java.beans.PropertyVetoException;
import java.io.*;
        import java.util.*;

import com.ibm.as400.access.*;

public class listJobs extends Object
{
    public static void main(String[] parameters)
    {
        listJobs me = new listJobs();
        me.Main(parameters);

        System.exit(0);
    }


    void Main(String[] parameters)
    {

        // If a system was not specified, display help text and exit.
        if (parameters.length == 0)
        {
            showHelp();
            return;
        }

        // Set up AS400 object parms.  The first is the system name and must
        // be specified by the user.  The second and third are optional.  They
        // are the userid and password.  Convert the userid and password
        // to uppercase before setting them on the AS400 object.
        String userID   = null;
        String password = null;

        if (parameters.length > 1)
            userID = parameters[1].toUpperCase();

        if (parameters.length >= 2)
            password = parameters[2];

        System.out.println(" ");

        try
        {

            // Create an AS400 object using the system name specified by the user.
            AS400 as400 = new AS400(parameters[0]);

            // If a userid and/or password was specified, set them on the
            // AS400 object.
            if (userID != null)
                as400.setUserId(userID);

            if (password != null)
                as400.setPassword(password);

            isCollectionServicesRunning(as400);
            readFile(as400, "/QSYS.LIB/QPFRDATA.LIB/QAPMSQLPC.FILE");


//            // ---------------- GET ALL THE JOBS ---------------- //
//            // Create a job list object.  Input parm is the AS400 we want job
//            // information from.
//            JobList jobList = new JobList(as400);
//
//            // Get a list of jobs running on the server.
//            Enumeration listOfJobs = jobList.getJobs();
//
//            // For each job in the list print information about the job.
//            while (listOfJobs.hasMoreElements())
//            {
//                printJobInfo((Job) listOfJobs.nextElement(), as400);
//            }
//            // ---------------- GET ALL THE JOBS ---------------- //

        }
        catch (Exception e)
        {
            System.out.println("Unexpected error");
            System.out.println(e);
        }
    }

    void readFile(AS400 as400, String filename) {
        try {

//            Optional<IFSFile> latestMember = retrieveLatestMember(as400, filename);
//
//            SequentialFile myFile =
//                    new SequentialFile(as400, latestMember.get().getAbsolutePath());

            QSYSObjectPathName qsysObjectPathName = new QSYSObjectPathName("QPFRDATA", "QAPMISUM", "*LAST", "MBR");
            SequentialFile myFile =
                    new SequentialFile(as400, qsysObjectPathName.getPath());

            AS400FileRecordDescription recordDescription =
                    new AS400FileRecordDescription(as400, qsysObjectPathName.getPath());

            myFile.setRecordFormat(recordDescription.retrieveRecordFormat()[0]);

            myFile.open(AS400File.READ_ONLY, 0, AS400File.COMMIT_LOCK_LEVEL_NONE);
            Record lastRecord = myFile.readLast();

            System.out.println(lastRecord);
            myFile.close();

        } catch (AS400SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (AS400Exception e) {
            e.printStackTrace();
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    Optional<IFSFile> retrieveLatestMember(AS400 as400, String filename) {
        try {
            IFSFile file = new IFSFile(as400, filename);
            Optional<IFSFile> newestMemberOptional = Arrays.stream(file.listFiles()).max(Comparator.comparing(file1 -> {
                try {
                    return file1.created();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return Long.MIN_VALUE;
            }));

            return newestMemberOptional;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    void isCollectionServicesRunning(AS400 as400) {
        CommandCall command = new CommandCall(as400);
        try {
            if (!command.run("CHKPFRCOL")) {
                System.out.println("COMMAND FAILED.");
            }

            AS400Message[] messagelist = command.getMessageList();
            for (int i = 0; i < messagelist.length; ++i)
            {
                // Show each message.
                System.out.println("Message: " + messagelist[0].getText());
                System.out.println("Help: " + messagelist[0].getHelp());
            }

        } catch (AS400SecurityException e) {
            e.printStackTrace();
        } catch (ErrorCompletingRequestException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    void retrieveCollectionServicesLocation() {

    }



    void printJobInfo(Job job, AS400 as400)
    {

        // Create the various converters we need
        AS400Bin4 bin4Converter   = new AS400Bin4( );
        AS400Text text26Converter = new AS400Text(26, as400);
        AS400Text text16Converter = new AS400Text(16, as400);
        AS400Text text10Converter = new AS400Text(10, as400);
        AS400Text text8Converter  = new AS400Text(8,  as400);
        AS400Text text6Converter  = new AS400Text(6,  as400);
        AS400Text text4Converter  = new AS400Text(4,  as400);


        // We have the job name/number/etc. from the list request.  Now
        // make a server API call to get the status of the job.
        try
        {
            // Create a program call object
            ProgramCall pgm = new ProgramCall(as400);

            // The server program we call has five parameters
            ProgramParameter[] parmlist = new ProgramParameter[5];

            // The first parm is a byte array that holds the output
            // data.  We will allocate a 1k buffer for output data.
            parmlist[0] = new ProgramParameter( 1024 );

            // The second parm is the size of our output data buffer (1K).
            Integer iStatusLength = new Integer( 1024 );
            byte[]  statusLength = bin4Converter.toBytes( iStatusLength );
            parmlist[1] = new ProgramParameter( statusLength );

            // The third parm is the name of the format of the data.
            // We will use format JOBI0200 because it has job status.
            byte[] statusFormat = text8Converter.toBytes("JOBI0200");
            parmlist[2] = new ProgramParameter( statusFormat );

            // The fourth parm is the job name is format "name user number".
            // Name must be 10 characters, user must be 10 characters and
            // number must be 6 characters.  We will use a text converter
            // to do the conversion and padding.
            byte[] jobName = text26Converter.toBytes(job.getName());

            int    i       = text10Converter.toBytes(job.getUser(),
                    jobName,
                    10);

            i       = text6Converter.toBytes(job.getNumber(),
                    jobName,
                    20);

            parmlist[3] = new ProgramParameter( jobName );

            // The last paramter is job identifier.  We will leave this blank.
            byte[] jobID = text16Converter.toBytes("                ");
            parmlist[4] = new ProgramParameter( jobID );


            // Run the program.
            if (pgm.run( "/QSYS.LIB/QUSRJOBI.PGM", parmlist )==false)
            {
                // if the program failed display the error message.
                AS400Message[] msgList = pgm.getMessageList();
                System.out.println(msgList[0].getText());
            }
            else
            {
                // else the program worked.  Output the status followed by
                // the jobName.user.jobID
                byte[] as400Data = parmlist[0].getOutputData();
                System.out.print("  " + text4Converter.toObject(as400Data, 107) + "  ");

                System.out.println(job.getName().trim() + "." +
                        job.getUser().trim() + "." +
                        job.getNumber() + "   ");
            }

        }
        catch (Exception e)
        {
            System.out.println(e);
        }

    }



    // Display help text when parameters are incorrect.
    void showHelp()
    {
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("Parameters are not correct.  Command syntax is:");
        System.out.println("");
        System.out.println("   listJobs System UserID Password");
        System.out.println("");
        System.out.println("Where");
        System.out.println("");
        System.out.println("   System   = server to connect to");
        System.out.println("   UserID   = valid userID on that system (optional)");
        System.out.println("   Password = password for the UserID (optional)");
        System.out.println("");
        System.out.println("For example:");
        System.out.println("");
        System.out.println("   listJobs MYAS400 JavaUser pwd1");
        System.out.println("");
        System.out.println("");
    }
}