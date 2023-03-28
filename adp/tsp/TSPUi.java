package adp.tsp;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;

/**
 * The UI application that provides the window and has a main method.
 */
public class TSPUi extends JFrame {

  private static final long serialVersionUID = 1L;

  private static int MINIMUM_NUMBER_OF_CITIES = 4;
  
  private final BufferedImage image;
  private final ImagePanel imagePanel = new ImagePanel();
  private final JComboBox<String> numberOfCitiesCombo;
  private final JButton goButton = new JButton("Go");
  private final JButton cancelButton = new JButton("Cancel");
  private final JButton replayButton = new JButton("Replay longest to shortest");
  private boolean isCanceled = false;
  
  private final SortedSet<TSPRoute> allRoutes = new TreeSet<>();

  public boolean isCanceled() {
    return isCanceled;
  }

  public void setCanceled(boolean canceled) {
    isCanceled = canceled;
  }

  public TSPUi(final int width, final int height) {

//    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);    
    this.imagePanel.setImage(this.image);
    
    final JPanel mainPanel = new JPanel(new BorderLayout());
    final JPanel topPanel = new JPanel();
    
    final String[] cities = new String[6];
    for (int i = 0; i < cities.length; i++) {
      cities[i] = i + MINIMUM_NUMBER_OF_CITIES + " cities";
    }
    this.numberOfCitiesCombo = new JComboBox<String>(cities);

//    By running the repaints in a background thread, we ensure that the UI
//    thread remains responsive to user input. However, note that doing a lot of
//    repaints can still affect the overall performance of the application.
    this.goButton.addActionListener((ev)-> {

      new Thread(new Runnable() {

        @Override
        public void run() {
          runAnimation();
        }
      }).start();
    });

    this.cancelButton.addActionListener((ev)->cancel());//todo

    this.replayButton.addActionListener((ev)-> {
      new Thread(new Runnable() {
        @Override
        public void run() {
          showLongestToShortest();
        }
      }).start();
    });
    this.cancelButton.setEnabled(true);//todo false
    this.replayButton.setEnabled(false);

    topPanel.add(this.numberOfCitiesCombo);
    topPanel.add(this.goButton);
    topPanel.add(this.cancelButton);
    topPanel.add(this.replayButton);

    mainPanel.add(topPanel, BorderLayout.NORTH);
    mainPanel.add(this.imagePanel, BorderLayout.CENTER);
    
    add(mainPanel);
    pack();
    setVisible(true);
    
  }


  private void runAnimation() {
    final int numberOfCities = this.numberOfCitiesCombo.getSelectedIndex() + MINIMUM_NUMBER_OF_CITIES;
    this.goButton.setEnabled(false);
    this.replayButton.setEnabled(false);
    this.cancelButton.setEnabled(true);
    this.allRoutes.clear();
    this.imagePanel.resetPaintCallCounter();
    final TSP tsp = new TSP(numberOfCities, TSPUi.this.image.getWidth(), TSPUi.this.image.getHeight());
    tsp.setListener(new UIListener(TSPUi.this));
    tsp.findShortestRoute();

  }

  private void cancel() {
          
    System.out.println( "Cancelling...");
    this.goButton.setEnabled(true);
    this.cancelButton.setEnabled(false);
    this.setCanceled(true);
  }
  
  private void showLongestToShortest() {
    this.replayButton.setEnabled(false);
    this.cancelButton.setEnabled(true);
    this.goButton.setEnabled(false);
    
    for(final TSPRoute route : TSPUi.this.allRoutes) {
      if(this.isCanceled) break;
      displayOneRouteEDT(route, Color.WHITE);
    }      
    displayBestRouteEDT(TSPUi.this.allRoutes.last());
    
  }

  public void displayBestRouteEDT(final TSPRoute bestRoute) {
    try {
      SwingUtilities.invokeAndWait(()->displayBestRoute(bestRoute));
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  public void displayBestRoute(final TSPRoute bestRoute) {
    displayOneRoute(bestRoute, Color.GREEN);
    System.out.println("Paint calls: " + this.imagePanel.paintCalls());
    this.goButton.setEnabled(true);
    this.cancelButton.setEnabled(true); //todo false
    this.replayButton.setEnabled(true);
    this.setCanceled(false);
  }

  private void displayOneRouteEDT(final TSPRoute bestRoute, final Color color) {
    try {
      SwingUtilities.invokeAndWait(() -> displayOneRoute(bestRoute, color));
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  private void displayOneRoute(final TSPRoute bestRoute, final Color color) {
    final Graphics2D g = (Graphics2D) this.image.getGraphics();
    g.setFont(Font.decode("SANSSERIF-BOLD-24"));
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());
    
    g.setColor(Color.BLACK);
    g.drawString(String.format("%.2f", bestRoute.distance()), 5, 24);

    g.setColor(color);
    g.setStroke(new BasicStroke(7f));
    paintPath(g, bestRoute);

    paintLocations(bestRoute.route(), g);

    this.imagePanel.repaint();
//    this.imagePanel.paintImmediately(0,0,this.imagePanel.getWidth(), this.imagePanel.getHeight());
  }

  public void displayRouteUpdateEDT(final TSPRoute route, final TSPRoute bestRoute) {
    try {
      SwingUtilities.invokeAndWait(()->displayRouteUpdate(route, bestRoute));
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  public void displayRouteUpdate(final TSPRoute route, final TSPRoute bestRoute) {
    this.allRoutes.add(route);
    final Graphics2D g = (Graphics2D) this.image.getGraphics();
    g.setFont(Font.decode("SANSSERIF-BOLD-24"));
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());
    
    g.setColor(Color.BLACK);
    g.drawString(String.format("%.2f (%.2f)", route.distance(), bestRoute.distance()), 5, 24);

    g.setColor(Color.WHITE); 
    g.setStroke(new BasicStroke(7f));
    paintPath(g, bestRoute);

    g.setColor(Color.DARK_GRAY);
    g.setStroke(new BasicStroke(1f));
    paintPath(g, route);

    paintLocations(route.route(), g);

    // this call to paintImmediately is used to ensure that every call to displayUpdate
    // actually gets painted - if we used the asynchronous repaint() method as is more
    // usual, paint jobs could be discarded when a new paint job arrived, resulting in
    // just the last update actually being displayed. However, this approach means that
    // thousands of repaint events are on the edt and user interactivity is blocked - so no good!
//    this.imagePanel.paintImmediately(0,0,this.imagePanel.getWidth(), this.imagePanel.getHeight());
//    SwingUtilities.invokeLater(()->this.imagePanel.paintImmediately(0,0,this.imagePanel.getWidth(), this.imagePanel.getHeight()));
//    SwingUtilities.invokeLater(()->this.imagePanel.paintImmediately(0,0,this.imagePanel.getWidth(), this.imagePanel.getHeight()));
//    this.imagePanel.paintImmediately(0,0,this.imagePanel.getWidth(), this.imagePanel.getHeight());
    this.imagePanel.paintCalls ++;
    this.imagePanel.repaint();
  }

  private void paintLocations(final Point[] locations, final Graphics2D g) {
    g.setColor(Color.MAGENTA);
    for(final Point p : locations) {
      g.fillOval(p.x - 5, p.y - 5, 15, 15);
      g.setColor(Color.BLUE);
    }
  }
  
  private void paintPath(final Graphics2D g, final TSPRoute path) {
    final Point[] locations = path.route();
    for (int i = 1; i < locations.length; i++) {
      final Point s = locations[i - 1];
      final Point e = locations[i];
      g.drawLine(s.x, s.y, e.x, e.y);
    }
    final Point s = locations[locations.length - 1];
    final Point e = locations[0];
    g.drawLine(s.x, s.y, e.x, e.y);    
  }
  
  public static void launch() {
    new TSPUi(500, 500);
  }
  
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(()->launch());
  }
  
}
