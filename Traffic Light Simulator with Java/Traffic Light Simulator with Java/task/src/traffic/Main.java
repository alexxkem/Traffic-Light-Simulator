package traffic;
import java.util.Scanner;

class CircularQueue {
  private final String[] roads;
  private final int capacity;
  private int head = 0;
  private int tail = 0;
  private int size = 0;

  public CircularQueue(int capacity) {
    this.capacity = capacity;
    this.roads = new String[capacity];
  }

  public boolean enqueue(String road) {
    if (size == capacity) {
      System.out.println("Queue is full");
      return false;
    }

    roads[tail] = road;
    tail = (tail + 1) % capacity;
    size++;

    System.out.println(road + " added!");
    return true;
  }

  public boolean dequeue() {
    if (isEmpty()) {
      System.out.println("Queue is empty");
      return false;
    }

    String deletedRoad = roads[head];
    roads[head] = null;
    head = (head + 1) % capacity;
    size--;

    System.out.println(deletedRoad + " deleted!");
    return true;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public int size() {
    return size;
  }

  public String get(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException();
    }

    return roads[(head + index) % capacity];
  }
}

public class Main {
  private static final Scanner input = new Scanner(System.in);
  private static final Object LOCK = new Object();

  private static volatile boolean running = true;
  private static volatile boolean isSystemState = false;

  private static int numberRoads;
  private static int interval;
  private static int seconds = 0;
  private static int timeRemaining;

  private static final String ANSI_RED = "\u001B[31m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_YELLOW = "\u001B[33m";
  private static final String ANSI_RESET = "\u001B[0m";

  private static int openIndex = -1;

  private static CircularQueue queue;

  public static void main(String[] args) {
    System.out.println("Welcome to the traffic management system!");

    readInitialSettings();
    clearOutput();
    startSystemThread();

    while (running) {
      menu();
      String option = input.nextLine();

      try {
        inputHandler(Integer.parseInt(option));
      } catch (NumberFormatException e) {
        System.out.println("Incorrect option");
        waitForEnter();
      }

      clearOutput();
    }
  }

  private static void readInitialSettings() {
    numberRoads = readPositiveInt("Input the number of roads: ");
    interval = readPositiveInt("Input the interval: ");

    queue = new CircularQueue(numberRoads);
    timeRemaining = interval;
  }

  private static int readPositiveInt(String prompt) {
    System.out.print(prompt);

    while (true) {
      String line = input.nextLine();

      try {
        int value = Integer.parseInt(line);
        if (value > 0) {
          return value;
        }
      } catch (NumberFormatException ignored) {
      }

      System.out.print("Error! Incorrect Input. Try again: ");
    }
  }

  private static void startSystemThread() {
    Thread queueThread = new Thread(() -> {
      while (running) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          return;
        }

        synchronized (LOCK) {
          seconds++;

          if (isSystemState) {
            printSystemState();
          }

          updateTrafficState();
        }
      }
    });

    queueThread.setName("QueueThread");
    queueThread.setDaemon(true);
    queueThread.start();
  }

  private static void updateTrafficState() {
    if (queue.isEmpty()) {
      openIndex = -1;
      timeRemaining = interval;
      return;
    }

    timeRemaining--;

    if (timeRemaining <= 0) {
      timeRemaining = interval;

      if (openIndex == -1) {
        openIndex = 0;
      } else if (queue.size() > 1) {
        openIndex = (openIndex + 1) % queue.size();
      } else {
        openIndex = 0;
      }
    }
  }

  private static void printSystemState() {
    clearOutput();

    System.out.println("! " + seconds + "s. have passed since system startup !");
    System.out.println("! Number of roads: " + numberRoads + " !");
    System.out.println("! Interval: " + interval + " !");
    System.out.println();

    displayRoads();

    System.out.println();
    System.out.println("! Press \"Enter\" to open menu !");
  }

  private static void displayRoads() {
    int size = queue.size();

    for (int i = 0; i < size; i++) {
      String road = queue.get(i);
      boolean isOpen = i == openIndex;
      int remaining;

      if (openIndex == -1) {
        remaining = timeRemaining + i * interval;
      } else if (isOpen) {
        remaining = timeRemaining;
      } else {
        int distanceFromOpenRoad = (i - openIndex + size) % size;
        remaining = timeRemaining + (distanceFromOpenRoad - 1) * interval;
      }

      String status = isOpen
              ? ANSI_GREEN + "open for " + remaining + "s." + ANSI_RESET
              : ANSI_RED + "closed for " + remaining + "s." + ANSI_RESET;

      System.out.println("Road \"" + road + "\" will be " + status);
    }
  }

  private static void inputHandler(int selectedInput) {
    switch (selectedInput) {
      case 1:
        System.out.print("Input road name: ");
        String road = input.nextLine();

        synchronized (LOCK) {
          boolean wasEmpty = queue.isEmpty();
          boolean wasAdded = queue.enqueue(road);

          if (wasAdded && wasEmpty) {
            openIndex = 0;
            timeRemaining = interval;
          }
        }

        waitForEnter();
        break;

      case 2:
        synchronized (LOCK) {
          if (queue.isEmpty()) {
            queue.dequeue();
          } else {
            boolean deletedOpenRoad = openIndex == 0;
            boolean deletedRoadBeforeOpenRoad = openIndex > 0;

            boolean wasDeleted = queue.dequeue();

            if (wasDeleted) {
              if (queue.isEmpty()) {
                openIndex = -1;
                timeRemaining = interval;
              } else if (deletedOpenRoad) {
                openIndex = -1;
              } else if (deletedRoadBeforeOpenRoad) {
                openIndex--;
              }
            }
          }
        }

        waitForEnter();
        break;

      case 3:
        isSystemState = true;
        input.nextLine();
        isSystemState = false;
        break;

      case 0:
        System.out.println("Bye!");
        running = false;
        break;

      default:
        System.out.println("Incorrect option");
        waitForEnter();
        break;
    }
  }

  private static void waitForEnter() {
    input.nextLine();
  }

  private static void clearOutput() {
    System.out.flush();
  }

  private static void menu() {
    System.out.println("Menu:");
    System.out.println("1. Add");
    System.out.println("2. Delete");
    System.out.println("3. System");
    System.out.println("0. Quit");
  }
}