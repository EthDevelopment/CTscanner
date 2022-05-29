import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.awt.*;
import java.io.*;

// OK this is not best practice - maybe you'd like to create
// a volume data class?
// I won't give extra marks for that though.

public class Example extends Application {
    short cthead[][][]; //store the 3D volume data set
    short min, max; //min/max value in the 3D volume data set
    int CT_x_axis = 256;
    int CT_y_axis = 256;
    int CT_z_axis = 113;

    @Override
    public void start(Stage stage) throws FileNotFoundException, IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("BrainScanner.fxml"));
    	stage.setTitle("BrainScanner");

        ReadData();

        //Good practice: Define your top view, front view and side view images (get the height and width correct)
        //Here's the top view - looking down on the top of the head (each slice we are looking at is CT_x_axis x CT_y_axis)
        int Top_width = CT_x_axis;
        int Top_height = CT_y_axis;

        //Here's the front view - looking at the front (nose) of the head (each slice we are looking at is CT_x_axis x CT_z_axis)
        int Front_width = CT_x_axis;
        int Front_height = CT_z_axis;

        //and you do the other (side view) - looking at the ear of the head
        int Side_width = CT_y_axis;
        int Side_height = CT_z_axis;

        //We need 3 things to see an image
        //1. We create an image we can write to
        WritableImage top_image = new WritableImage(Top_width, Top_height);
        WritableImage Front_image = new WritableImage(Front_width, Front_height);
        WritableImage Side_image = new WritableImage(Side_width, Side_height);

        //2. We create a view of that image
        ImageView TopView = new ImageView(top_image);
        ImageView FrontView = new ImageView(Front_image);
        ImageView SideView = new ImageView(Side_image);

        // These create my buttons on screen
        Button slice_button = new Button("slice"); //an example button to slice
        Button Volrend_button = new Button("Vol rend"); // Vol rend button
        Button DB_button = new Button("Depth Shader 3000");

        //sliders to step through the slices (top and front directions) (remember 113 slices in top direction 0-112)
        Slider Top_slider = new Slider(0, CT_z_axis - 1, 0);
        Slider Front_slider = new Slider(0, CT_y_axis - 1, 0);
        Slider Side_Slider = new Slider(0, CT_x_axis - 1, 0);
        //q2
        Slider Opacity_slider = new Slider(0, 100, 0);

        DB_button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TopdownDB(top_image);
                FrontDB(Front_image );
                SideDB(Side_image);
            }
        });

        slice_button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TopDownSlice(top_image, 0);
                FrontSlice(Front_image, 0);
                SideSlice(Side_image, 0);
            }
        });

        Opacity_slider.valueProperty().addListener(
                new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number>
                                                observable, Number oldValue, Number OpacityValue) {
                        //	final Label OpacityLevel = new Label("Opacity Level");
                        double NewOpacity = OpacityValue.doubleValue() / 100;
                        System.out.println("Opacity level " + NewOpacity);
                        TopdownVolRend(top_image, NewOpacity);
                        FrontVolRend(Front_image, NewOpacity);
                        SideVolRend(Side_image, NewOpacity);
                    }
                });

        Top_slider.valueProperty().addListener(
                new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number>
                                                observable, Number oldValue, Number newValue) {
                        TopDownSlice(top_image, newValue.intValue());
                        System.out.println("TopView " + newValue.intValue());
                        Volrend_button.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                TopdownVolRend(top_image, newValue.intValue());
                            }
                        });
                    }
                });

        Volrend_button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TopdownVolRend(top_image,  1);
                FrontVolRend(Front_image, 1);
                SideVolRend(Side_image, 1);
            }
        });

        Front_slider.valueProperty().addListener(
                new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number>
                                                observable, Number oldValue, Number FrontValue) {
                        {
                            FrontSlice(Front_image, FrontValue.intValue());
                            System.out.println("FrontView " + FrontValue.intValue());
                            Volrend_button.setOnAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent event) {
                                    FrontVolRend(Front_image, FrontValue.intValue());
                                }
                            });
                        }
                    }
                }
        );

        Side_Slider.valueProperty().addListener(
                new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number>
                                                observable, Number oldValue, Number SideValue) {
                        {
                            SideSlice(Side_image, SideValue.intValue());
                            System.out.println("SideView " + SideValue.intValue());
                            Volrend_button.setOnAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent event) {
                                    SideVolRend(Side_image, SideValue.intValue());
                                }
                            });
                        }
                    }
                }
        );

        FlowPane root = new FlowPane();
        root.setVgap(8);
        root.setHgap(8);
        //https://examples.javacodegeeks.com/desktop-java/javafx/scene/image-scene/javafx-image-example/

        //3. (referring to the 3 things we need to display an image)
        //we need to add it to the flow pane
        root.getChildren().addAll(TopView, slice_button, Top_slider, FrontView, Front_slider, SideView, Side_Slider, Volrend_button, Opacity_slider,DB_button);


        Scene scene = new Scene(root, 500, 600);
        stage.setScene(scene);
        stage.show();
    }

    //Function to read in the cthead data set
    public void ReadData() throws IOException {
        //File name is hardcoded here - much nicer to have a dialog to select it and capture the size from the user
        File file = new File("CThead");
        //Read the data quickly via a buffer (in C++ you can just do a single fread - I couldn't find if there is an equivalent in Java)
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

        int i, j, k; //loop through the 3D data set

        min = Short.MAX_VALUE;
        max = Short.MIN_VALUE; //set to extreme values
        short read; //value read in
        int b1, b2; //data is wrong Endian (check wikipedia) for Java so we need to swap the bytes around

        cthead = new short[CT_z_axis][CT_y_axis][CT_x_axis]; //allocate the memory - note this is fixed for this data set
        //loop through the data reading it in
        for (k = 0; k < CT_z_axis; k++) {
            for (j = 0; j < CT_y_axis; j++) {
                for (i = 0; i < CT_x_axis; i++) {
                    //because the Endianess is wrong, it needs to be read byte at a time and swapped
                    b1 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types
                    b2 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types
                    read = (short) ((b2 << 8) | b1); //and swizzle the bytes around
                    if (read < min) min = read; //update the minimum
                    if (read > max) max = read; //update the maximum
                    cthead[k][j][i] = read; //put the short into memory (in C++ you can replace all this code with one fread)
                }
            }
        }
        System.out.println(min + " " + max); //diagnostic - for CThead this should be -1117, 2248
        //(i.e. there are 3366 levels of grey (we are trying to display on 256 levels of grey)
        //therefore histogram equalization would be a good thing
        //maybe put your histogram equalization code here to set up the mapping array
    }

    /*
       This function shows how to carry out an operation on an image.
       It obtains the dimensions of the image, and then loops through
       the image carrying out the copying of a slice of data into the
       image.
   */
    public void TopDownSlice(WritableImage image, int SliceID) {
        //Get image dimensions, and declare loop variables
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();
        double col;
        short datum;
        //Shows how to loop through each pixel and colour
        //Try to always use j for loops in y, and i for loops in x
        //as this makes the code more readable
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                //at this point (i,j) is a single pixel in the image
                //here you would need to do something to (i,j) if the image size
                //does not match the slice size (e.g. during an image resizing operation
                //If you don't do this, your j,i could be outside the array bounds
                //In the framework, the image is 256x256 and the data set slices are 256x256
                //so I don't do anything - this also leaves you something to do for the assignment
                datum = cthead[SliceID][j][i]; //get values from slice 76 (change this in your assignment)
                //calculate the colour by performing a mapping from [min,max] -> 0 to 1 (float)
                //Java setColor uses float values from 0 to 1 rather than 0-255 bytes for colour
                col = (((float) datum - (float) min) / ((float) (max - min)));
                image_writer.setColor(i, j, Color.color(col, col, col, 1.0));
            } // column loop
        } // row loop
    }

    public void FrontSlice(WritableImage image, int FrontSliceID) {
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        double col;
        short datum;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                datum = cthead[j][FrontSliceID][i];
                col = (((float) datum - (float) min) / ((float) (max - min)));
                image_writer.setColor(i, j, Color.color(col, col, col, 1.0));
            }
        }
    }

    public void SideSlice(WritableImage image, int SideSliceID) {
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        double col;
        short datum;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                datum = cthead[j][i][SideSliceID];
                col = (((float) datum - (float) min) / ((float) (max - min)));
                image_writer.setColor(i, j, Color.color(col, col, col, 1.0));
            }
        }
    }

    public void TopdownVolRend(WritableImage image, double NewOpacity) {
        PixelWriter image_writer = image.getPixelWriter();
        for (int j = 0; j < CT_y_axis; j++) {
            for (int i = 0; i < CT_x_axis; i++) {
                //𝐶𝐶𝑎𝑎𝑎𝑎𝑎𝑎𝑜𝑜𝑎𝑎= 𝐶𝐶𝑎𝑎𝑎𝑎𝑎𝑎𝑜𝑜𝑎𝑎 + 𝛼𝛼𝑎𝑎𝑎𝑎𝑎𝑎𝑜𝑜𝑎𝑎σ𝐿𝐿𝐶𝐶, 𝛼𝛼𝑎𝑎𝑎𝑎𝑎𝑎𝑜𝑜𝑎𝑎 = 𝛼𝛼𝑎𝑎𝑎𝑎𝑎𝑎𝑜𝑜𝑎𝑎 × (1 − σ)
                //Color accum = color accum + Transparency * sigma * Light value * Color
                double sigma;
                double Lvalue = 1.0;
                double red = 0;
                double green = 0;
                double blue = 0;
                double Transparency = 1.0;
                for (int k = 0; k < CT_z_axis; k++) {
                    double CTvalue;
                    CTvalue = cthead[k][j][i];
                    if (CTvalue < -300) {
                        sigma = 0;
                        red = red + Transparency * sigma * Lvalue * 0;
                        green = green + Transparency * sigma * Lvalue * 0;
                        blue = blue + Transparency * sigma * Lvalue * 0;
                        //a accum = a accum x (1 - sigma)
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }
                    } else if (CTvalue >= -300 && CTvalue <= 49) {
                        sigma = 0.12 * NewOpacity;
                        red = red + Transparency * sigma * Lvalue * 1;
                        green = green + Transparency * sigma * Lvalue * 0.79;
                        blue = blue + Transparency * sigma * Lvalue * 0.6;
                        Transparency = Transparency * (1 - sigma);

                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }

                    } else if (CTvalue >= 50 && CTvalue <= 299) {
                        sigma = 0;
                        red = red + Transparency * sigma * Lvalue * 0;
                        green = green + Transparency * sigma * Lvalue * 0;
                        blue = blue + Transparency * sigma * Lvalue * 0;
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }

                    } else if (CTvalue >= 300 && CTvalue <= 4096) {
                        sigma = 0.8 ;
                        ;
                        red = red + Transparency * sigma * Lvalue * 1;
                        green = green + Transparency * sigma * Lvalue * 1;
                        blue = blue + Transparency * sigma * Lvalue * 1;
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }
                    }
                }
                image_writer.setColor(i, j, Color.color(red, green, blue, 1));
            }
        }
    }

    public void FrontVolRend(WritableImage image, double NewOpacity) {
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        for (int j = 0; j < CT_x_axis; j++) {
            for (int i = 0; i < CT_z_axis; i++) {
                //𝐶𝐶𝑎𝑎𝑎𝑎𝑎𝑎𝑜𝑜𝑎𝑎= 𝐶𝐶𝑎𝑎𝑎𝑎𝑎𝑎𝑜𝑜𝑎𝑎 + 𝛼𝛼𝑎𝑎𝑎𝑎𝑎𝑎𝑜𝑜𝑎𝑎σ𝐿𝐿𝐶𝐶, 𝛼𝛼𝑎𝑎𝑎𝑎𝑎𝑎𝑜𝑜𝑎𝑎 = 𝛼𝛼𝑎𝑎𝑎𝑎𝑎𝑎𝑜𝑜𝑎𝑎 × (1 − σ)
                //Color accum = color accum + Transparency * sigma * Light value * Color

                double sigma = 0;
                double Lvalue = 1.0;
                double red = 0;
                double green = 0;
                double blue = 0;
                double Transparency = 1.0;

                for (int k = 0; k < CT_y_axis; k++) {
                    double CTvalue;
                    CTvalue = cthead[i][k][j];
                    if (CTvalue < -300) {
                        //	red = CompositingFunction(0,Lvalue,0,Transparency);
                        //	green = CompositingFunction(0,Lvalue,0,Transparency);
                        //	blue = CompositingFunction(0,Lvalue,0,Transparency);

                        sigma = 0;
                        red = red + Transparency * sigma * Lvalue * 0;
                        green = green + Transparency * sigma * Lvalue * 0;
                        blue = blue + Transparency * sigma * Lvalue * 0.;
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }


                    } else if (CTvalue >= -300 && CTvalue <= 49) {
                        sigma = 0.12 * NewOpacity;
                        ;
                        red = red + Transparency * sigma * Lvalue * 1;
                        green = green + Transparency * sigma * Lvalue * 0.79;
                        blue = blue + Transparency * sigma * Lvalue * 0.6;
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }

                    } else if (CTvalue >= 50 && CTvalue <= 299) {
                        sigma = 0;
                        red = red + Transparency * sigma * Lvalue * 0;
                        green = green + Transparency * sigma * Lvalue * 0;
                        blue = blue + Transparency * sigma * Lvalue * 0;
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }

                    } else if (CTvalue >= 300 && CTvalue <= 4096) {
                        sigma = 0.8 ;
                        ;
                        red = red + Transparency * sigma * Lvalue * 1;
                        green = green + Transparency * sigma * Lvalue * 1;
                        blue = blue + Transparency * sigma * Lvalue * 1;
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }
                    }
                }
                image_writer.setColor(j, i, Color.color(red, green, blue, 1));
            }
        }
    }

    public void SideVolRend(WritableImage image, double NewOpacity) {
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        for (int j = 0; j < CT_y_axis; j++) {
            for (int i = 0; i < CT_z_axis; i++) {
                double sigma = 0;
                double Lvalue = 1.0;
                double red = 0;
                double green = 0;
                double blue = 0;
                double Transparency = 1.0;

                for (int k = 0; k < CT_x_axis; k++) {
                    double CTvalue;
                    CTvalue = cthead[i][j][k];
                    if (CTvalue < -300) {
                        //	red = CompositingFunction(0,Lvalue,0,Transparency);
                        //	green = CompositingFunction(0,Lvalue,0,Transparency);
                        //	blue = CompositingFunction(0,Lvalue,0,Transparency);
                        sigma = 0;
                        red = red + Transparency * sigma * Lvalue * 0;
                        green = green + Transparency * sigma * Lvalue * 0;
                        blue = blue + Transparency * sigma * Lvalue * 0.;
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }
                    } else if (CTvalue >= -300 && CTvalue <= 49) {
                        sigma = 0.12 * NewOpacity;
                        ;
                        red = red + Transparency * sigma * Lvalue * 1;
                        green = green + Transparency * sigma * Lvalue * 0.79;
                        blue = blue + Transparency * sigma * Lvalue * 0.6;
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }
                    } else if (CTvalue >= 50 && CTvalue <= 299) {
                        sigma = 0;
                        red = red + Transparency * sigma * Lvalue * 0;
                        green = green + Transparency * sigma * Lvalue * 0;
                        blue = blue + Transparency * sigma * Lvalue * 0;
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }
                    } else if (CTvalue >= 300 && CTvalue <= 4096) {
                        sigma = 0.8;
                        ;
                        red = red + Transparency * sigma * Lvalue * 1;
                        green = green + Transparency * sigma * Lvalue * 1;
                        blue = blue + Transparency * sigma * Lvalue * 1;
                        Transparency = Transparency * (1 - sigma);
                        if (red > 1) {
                            red = 1;
                        }
                        if (green > 1) {
                            green = 1;
                        }
                        if (blue > 1) {
                            blue = 1;
                        }
                    }
                }
                image_writer.setColor(j, i, Color.color(red, green, blue, 1));
            }
        }
    }

    public void TopdownDB(WritableImage image) {
        PixelWriter image_writer = image.getPixelWriter();
        for (int j = 0; j < CT_y_axis; j++) {
            for (int i = 0; i < CT_x_axis; i++) {
                double opacity = 1.0;
                double red = 0;
                double green = 0;
                double blue = 0;
                for (int k = 112; k > 0 ; k--){
                 double sliceValue =k;
                    int datum = cthead[k][j][i];
                    if (datum > 400){
                        red = 1.0-(sliceValue/112.0);
                        green = 1.0-(sliceValue/112.0);
                        blue = 1.0-(sliceValue/112.0);
                      opacity = 0;
                    }
                }
                image_writer.setColor(i, j, Color.color(red, green, blue, opacity));
            }
        }
    }

    public void FrontDB(WritableImage image) {
        PixelWriter image_writer = image.getPixelWriter();
        for (int j = 0; j < CT_x_axis; j++) {
            for (int i = 0; i < CT_z_axis; i++) {
                double opacity = 1.0;
                double red = 0;
                double green = 0;
                double blue = 0;
                for (int k = 255; k > 0 ; k--){
                    double sliceValue =k;
                    int datum = cthead[i][k][j];
                    if (datum > 400){
                        red = (1.0-sliceValue/225.0);
                        green = (1.0-sliceValue/225.0);
                        blue = 1-(sliceValue/225.0);
                        opacity = 0;

                    }
                    if (red > 1) {
                        red = 1;
                    }
                    if (green > 1) {
                        green = 1;
                    }
                    if (blue > 1) {
                        blue = 1;
                    }
                    if (red < 0) {
                        red = 0;
                    }
                    if (green < 0) {
                        green = 0;
                    }
                    if (blue < 0) {
                        blue = 0;
                    }
                }
                image_writer.setColor(j, i, Color.color(red, green, blue, opacity));
            }
        }
    }
    public void SideDB(WritableImage image) {
        PixelWriter image_writer = image.getPixelWriter();
        for (int j = 0; j < CT_y_axis; j++) {
            for (int i = 0; i < CT_z_axis; i++) {
                double opacity = 1.0;
                double red = 0;
                double green = 0;
                double blue = 0;
                for (int k = 255; k > 0 ; k--){
                    double sliceValue =k;
                    int datum = cthead[i][j][k];
                    if (datum > 400){
                        red = (1.0-sliceValue/225.0);
                        green = (1.0-sliceValue/225.0);
                        blue = 1-(sliceValue/225.0);
                        opacity = 0;

                    }
                    if (red > 1) {
                        red = 1;
                    }
                    if (green > 1) {
                        green = 1;
                    }
                    if (blue > 1) {
                        blue = 1;
                    }
                    if (red < 0) {
                        red = 0;
                    }
                    if (green < 0) {
                        green = 0;
                    }
                    if (blue < 0) {
                        blue = 0;
                    }
                }
                image_writer.setColor(j, i, Color.color(red, green, blue, opacity));
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }

}