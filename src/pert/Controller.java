package pert;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Scanner;

public class Controller {
    @FXML
    public TextField pathinput,desiredtime;
    public Button submit;
    public String path=null;
    @FXML
    private AnchorPane mainPane;
    public void inputfile(ActionEvent event) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int result = fileChooser.showOpenDialog(fileChooser);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
           path=selectedFile.getAbsolutePath();

        }
        pathinput.setText(path);
    }

    public void submit(ActionEvent event) throws Exception {
        CPM cpm=new CPM();
        Double dt= Double.valueOf(String.valueOf(desiredtime.getText()));
        cpm.setDt(dt);
        cpm.setPath(pathinput.getText());
        cpm.maincpm();

        AnchorPane pane=FXMLLoader.load(getClass().getResource("result.fxml"));
        TextField criticalpath=(TextField)pane.lookup("#criticalpath");
        TextField criticaltime=(TextField)pane.lookup("#criticaltime");
        TextField zvalue=(TextField)pane.lookup("#zvalue");
        TextField probability=(TextField)pane.lookup("#probability");
        TextField desttime=(TextField)pane.lookup("#desiredtime");
        criticalpath.setText(cpm.getCriticcalPath());
        criticaltime.setText(String.valueOf(cpm.getCt()));
        zvalue.setText(String.valueOf(cpm.getZ()));
        probability.setText(String.valueOf(cpm.getP()));
        desttime.setText(desiredtime.getText());

        criticalpath.setEditable(false);
        criticaltime.setEditable(false);
        zvalue.setEditable(false);
        probability.setEditable(false);
        desttime.setEditable(false);

        mainPane.getChildren().setAll(pane);



    }

}

class CPM {
    private static int na;
    private static double sd;
    private static double dt;
    private static Graph graph ;
    private static String path;
    private static String criticalpath=null;
    private static double z;
    private static double p;
    private static double ct;
    private static String lastid;

    public final String getCriticcalPath()
    {
        return criticalpath;
    }
    public final double getZ()
    {
        return z;
    }
    public final double getP()
    {
        return p;
    }
    public final double getCt()
    {
        return ct;
    }

    public final void setPath(String value)
    {
        path=value;
    }
    public final void setDt(Double value)
    {
        dt=value;
    }
    public  void maincpm() throws Exception {

        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        Activity[] list;
        list = GetActivities();
        list = WalkListAhead(list);
        list = WalkListAback(list);
        //print(list);
        CriticalPath(list);
    }
    private static Activity[] GetActivities() throws IOException {


        File file = new File(path);
        Scanner input = new Scanner(file);
        do
        {
            LineNumberReader lnr = new LineNumberReader(new FileReader(new File(path)));
            lnr.skip(Long.MAX_VALUE);
            na=lnr.getLineNumber() + 1;
            lnr.close();
            if (na < 2)
            {
                System.out.print("\n Invalid entry. The number must be >= 2.\n");
            }
        } while (na < 2);
        int index=0;
        String[][] animal = new String[na][];
        while (input.hasNextLine() && index < animal.length) {
            animal[index] = input.nextLine().split("#"); //split returns an array
            index++;
        }
        Activity[] list = new Activity[na];

        String [] last=animal[na-1];
        lastid=last[0];
        for (int i = 0; i < na; i++)
        {
            String[] a=animal[i];
            Activity activity = new Activity();

            activity.setId(a[0]);

            activity.setTo(Double.parseDouble(a[1]));

            activity.setTm(Double.parseDouble(a[2]));

            activity.setTp(Double.parseDouble(a[3]));

            activity.setDuration((activity.getTo() + (4 * activity.getTm()) + activity.getTp()) / 6);
            activity.setV((activity.getTp() - activity.getTo()) / 6);
            activity.setV(activity.getV() * activity.getV());


            int np= Integer.parseInt(a[4]);
            if (np != 0)
            {
                activity.setPredecessors(new Activity[np]);

                String id;

                for (int j = 0; j < np; j++)
                {
                    id=a[4+j+1];
                    Activity aux = new Activity();

                    if ((aux = aux.CheckActivity(list, id, i)) != null)
                    {
                        activity.getPredecessors()[j] = aux;

                        list[aux.GetIndex(list, aux, i)] = aux.SetSuccessors(aux, activity);

                    }
                    else
                    {

                        System.out.print("\n Error in predecesors of "+ a[0] +"\n\n");
                        j--;
                        System.exit(0);
                    }
                }
            }
            list[i] = activity;
        }

        return list;
    }


    private static Activity[] WalkListAhead(Activity[] list)
    {
        for (Activity activity:list)
        {
            if (activity.getPredecessors()==null)
            {
                activity.setEst(0);
                activity.setEet(activity.getDuration());
            }
        }

        list[0].setEet(list[0].getEst() + list[0].getDuration());

        for (int i = 1; i < na; i++)
        {
            if (list[i].getPredecessors()!=null) {
                for (Activity activity : list[i].getPredecessors()) {
                    if (list[i].getEst() < activity.getEet()) {
                        list[i].setEst(activity.getEet());
                    }
                }
            }
            list[i].setEet(list[i].getEst() + list[i].getDuration());
        }


        return list;
    }

    private static Activity[] WalkListAback(Activity[] list)
    {
        for (Activity activity:list)
        {
            if(activity.getSuccessors()==null)
            {
                ArrayList<Double> critic=new ArrayList<>();
                for (Activity problem:list)
                {
                    critic.add(problem.getEet());
                }
                activity.setLet(Collections.max(critic));
                activity.setLst(activity.getLet()-activity.getDuration());
            }
        }
        list[na - 1].setLet(list[na - 1].getEet());
        list[na - 1].setLst(list[na - 1].getLet() - list[na - 1].getDuration());

        for (int i = na - 2; i >= 0; i--)
        {
            if(list[i].getSuccessors()!=null) {
                for (Activity activity : list[i].getSuccessors()) {
                    if (list[i].getLet() == 0) {
                        list[i].setLet(activity.getLst());
                    } else {
                        if (list[i].getLet() > activity.getLst()) {
                            list[i].setLet(activity.getLst());
                        }
                    }
                }
            }
            list[i].setLst(list[i].getLet() - list[i].getDuration());
        }

        return list;
    }
    private static void print(Activity[] list) {
        System.out.format("%10s%10s%10s%10s%10s", "Activity","EST", "EET", "LST", "LET");
        System.out.println("");

        for(Activity activity:list)
        {
            System.out.format("%10s%10s%10s%10s%10s",activity.getId(), activity.getEst(), activity.getEet(), activity.getLst(), activity.getLet());
            System.out.println("");
        }
    }

    public static double err(double z)
    {

        double sum=0;
        for (int n=0;n<100;n++)
        {
            sum=sum+((Math.pow(-1,n)*Math.pow(z,(2*n)+1))/(((2*n)+1)*factorial(n)));
        }
        sum=(sum*2)/Math.sqrt(Math.PI);

        return sum;
    }
    public static double factorial(int number)
    {
        double fact=1;

        if (number==0)
            return 1;
        for(int i=1;i<=number;i++){
            fact=fact*i;
        }
        return fact;
    }
    private static void CriticalPath(Activity[] list)
    {
        graph= new SingleGraph("PERT");

        for (Activity activity:list)
        {
            graph.addNode(activity.getId());
        }
        for (Activity activity:list)
        {
            if (activity.getSuccessors()!=null)
            {
                for (Activity suc:activity.getSuccessors())
                {
                    String edge=""+activity.getId()+""+suc.getId();
                    graph.addEdge(edge,activity.getId(),suc.getId(),true);
                }
            }
        }
        for (Node node:graph)
        {
            node.addAttribute("ui.label", node.getId());
            graph.addAttribute("ui.stylesheet", "node { text-alignment: under; text-color: white; text-style: bold;text-size: 20; text-background-mode: rounded-box; text-background-color: #222C; text-padding: 1px; text-offset: 0px, 2px; } ");
        }

        ArrayList<Double>critic=new ArrayList<>();

        for (Activity activity : list)
        {
            if ((activity.getEet() - activity.getLet() == 0) && (activity.getEst() - activity.getLst() == 0))
            {

                criticalpath=criticalpath+activity.getId()+" ";
                sd = sd + activity.getV();
                graph.getNode(activity.getId()).addAttribute("ui.style"," size: 20px; fill-color: red; ");
            }
            critic.add(activity.getEet());

        }
        criticalpath=criticalpath.substring(4);
        sd = Math.sqrt(sd);
        ct = Collections.max(critic);
        z = (dt - ct) / sd;
        p=(1+err(z/Math.sqrt(2)))/2;

        graph.display();
    }


}

class Activity
{
    private String id;
    private double to;
    private double tm;
    private double tp;
    private double duration;
    private double est;
    private double lst;
    private double eet;
    private double let;
    private double v;
    private Activity[] successors;
    private Activity[] predecessors;

    public final String getId()
    {
        return id;
    }
    public final void setId(String value)
    {
        id = value;
    }


    public final double getDuration()
    {
        return duration;
    }
    public final void setDuration(double value)
    {
        duration = value;
    }

    public final double getEst()
    {
        return est;
    }
    public final void setEst(double value)
    {
        est = value;
    }

    public final double getLst()
    {
        return lst;
    }
    public final void setLst(double value)
    {
        lst = value;
    }

    public final double getEet()
    {
        return eet;
    }
    public final void setEet(double value)
    {
        eet = value;
    }

    public final double getLet()
    {
        return let;
    }
    public final void setLet(double value)
    {
        let = value;
    }
    public final double getTo()
    {
        return to;
    }
    public final void setTo(double value)
    {
        to = value;
    }
    public final double getTm()
    {
        return tm;
    }
    public final void setTm(double value)
    {
        tm = value;
    }
    public final double getTp()
    {
        return tp;
    }
    public final void setTp(double value)
    {
        tp = value;
    }
    public final double getV()
    {
        return v;
    }
    public final void setV(double value)
    {
        v = value;
    }

    public final Activity[] getPredecessors()
    {
        return predecessors;
    }
    public final void setPredecessors(Activity[] value)
    {
        predecessors = value;
    }

    public final Activity[] getSuccessors()
    {
        return successors;
    }
    public final void setSuccessors(Activity[] value)
    {
        successors = value;
    }


    public final Activity CheckActivity(Activity[] list, String id, int i)
    {
        for (int j = 0; j < i; j++)
        {
            if (list[j].getId().equals(id))
            {
                return list[j];
            }
        }
        return null;
    }
    public final int GetIndex(Activity[] list, Activity aux, int i)
    {
        for (int j = 1; j < i; j++)
        {
            if (list[j].getId().equals(aux.getId()))
            {
                return j;
            }
        }
        return 0;
    }

    public final Activity SetSuccessors(Activity aux, Activity activity)
    {
        if (aux.getSuccessors() != null)
        {
            Activity aux2 = new Activity();
            aux2.setSuccessors(new Activity[aux.getSuccessors().length + 1]);
            System.arraycopy(aux.getSuccessors(), 0, aux2.getSuccessors(), 0, aux.getSuccessors().length);
            aux2.getSuccessors()[aux.getSuccessors().length] = activity;
            aux.setSuccessors(aux2.getSuccessors());
        }
        else
        {
            aux.setSuccessors(new Activity[1]);
            aux.getSuccessors()[0] = activity;
        }
        return aux;
    }
}
