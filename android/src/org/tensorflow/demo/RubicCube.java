package org.tensorflow.demo;

/**
 * Created by sxy52 on 2018/3/9.
 */

public class RubicCube {
    private String[] cube_up = { "#FF0000", "#FF0000", "#FF0000", "#FF0000"};
    private String[] cube_down = { "#00FF00", "#00FF00", "#00FF00", "#00FF00"};
    private String[] cube_front = { "#CCCCFF", "#0000FF", "#0000FF", "#0000FF"};
    private String[] cube_back = { "#FFFF00", "#FFFF00", "#FFFF00", "#FFFF00"};
    private String[] cube_left = { "#FF00FF", "#FF00FF", "#FF00FF", "#FF00FF"};
    private String[] cube_right = { "#FFFFFF", "#FFFFFF", "#FFFFFF", "#FFFFFF"};

    private int prev_x=0, prev_y=0;
    private String prev_color = "#0000FF";
    private String[] hover = {"#FFCCCC", "#CCFFCC", "#CCCCFF", "#FFFFCC", "#FFCCFF", "#CCCCCC"};

    public RubicCube(){}

    public void resetHover(){
        int i = getCubeInd(prev_x, prev_y);
        cube_front[i] = prev_color;
    }

    public void setHover(int x, int y){
        prev_x = x;
        prev_y = y;
        int i = getCubeInd(prev_x, prev_y);
        prev_color = cube_front[i];
        switch(prev_color){
            case "#FF0000":
                cube_front[i] = hover[0];
                break;
            case "#00FF00":
                cube_front[i] = hover[1];
                break;
            case "#0000FF":
                cube_front[i] = hover[2];
                break;
            case "#FFFF00":
                cube_front[i] = hover[3];
                break;
            case "#FF00FF":
                cube_front[i] = hover[4];
                break;
            case "#FFFFFF":
                cube_front[i] = hover[5];
                break;
        }
    }

    public int getCubeInd(int x, int y){
        if(x == 0){
            if(y==0){
                return 0;
            }else{
                return 2;
            }
        }else{
            if(y==0){
                return 1;
            }else{
                return 3;
            }
        }
    }

    public void push_up(){
        // change the cplor of surface
        String[] temp = cube_up;
        cube_up = cube_front;
        cube_front = cube_down.clone();
        cube_down[3] = cube_back[0];
        cube_down[1] = cube_back[2];
        cube_down[2] = cube_back[1];
        cube_down[0] = cube_back[3];
        cube_back[0] = temp[3];
        cube_back[1] = temp[2];
        cube_back[2] = temp[1];
        cube_back[3] = temp[0];
        //rotate left
        left_rotate(true);
        //rotate right
        right_rotate(true);
    }

    public void push_down(){
        // change the cplor of surface
        String[] temp = cube_up.clone();
        cube_up[3] = cube_back[0];
        cube_up[1] = cube_back[2];
        cube_up[2] = cube_back[1];
        cube_up[0] = cube_back[3];
        cube_back[0] = cube_down[3];
        cube_back[1] = cube_down[2];
        cube_back[2] = cube_down[1];
        cube_back[3] = cube_down[0];
        cube_down = cube_front;
        cube_front = temp;
        //rotate left
        left_rotate(false);
        //rotate right
        right_rotate(false);
    }

    public void push_right(){
        // change the cplor of surface
        String[] temp = cube_left;
        cube_left = cube_back;
        cube_back = cube_right;
        cube_right = cube_front;
        cube_front = temp;
        //rotate left
        up_rotate(true);
        //rotate right
        down_rotate(true);
    }

    public void push_left(){
        // change the cplor of surface
        String[] temp = cube_left;
        cube_left = cube_front;
        cube_front = cube_right;
        cube_right = cube_back;
        cube_back = temp;
        //rotate left
        up_rotate(false);
        //rotate right
        down_rotate(false);
    }

    public void twist_up(int x){
        if(x == 0){
            String[] temp = {cube_up[0], cube_up[2]};
            String[] back = {cube_back[1], cube_back[3]};
            cube_up[0] = cube_front[0];
            cube_front[0] = cube_down[0];
            cube_down[0] = back[1];
            cube_back[1] = temp[1];
            cube_up[2] = cube_front[2];
            cube_front[2] = cube_down[2];
            cube_down[2] = back[0];
            cube_back[3] = temp[0];
            //rotate left
            left_rotate(true);
        }
        if(x == 1){
            String[] temp = {cube_up[1], cube_up[3]};
            String[] back = {cube_back[0], cube_back[2]};
            cube_up[1] = cube_front[1];
            cube_front[1] = cube_down[1];
            cube_down[1] = back[1];
            cube_back[0] = temp[1];
            cube_up[3] = cube_front[3];
            cube_front[3] = cube_down[3];
            cube_down[3] = back[0];
            cube_back[2] = temp[0];
            //rotate left
            right_rotate(true);
        }
    }

    public void twist_down(int x){
        if(x == 0){
            String[] temp = {cube_up[0], cube_up[2]};
            String[] back = {cube_back[1], cube_back[3]};
            String[] down = {cube_down[0], cube_down[2]};
            cube_up[0] = back[1];
            cube_back[1] = down[1];
            cube_down[0] = cube_front[0];
            cube_front[0] = temp[0];
            cube_up[2] = back[0];
            cube_back[3] = down[0];
            cube_down[2] = cube_front[2];
            cube_front[2] = temp[1];
            //rotate left
            left_rotate(false);
        }
        if(x == 1){
            String[] temp = {cube_up[1], cube_up[3]};
            String[] back = {cube_back[0], cube_back[2]};
            String[] down = {cube_down[1], cube_down[3]};
            cube_up[1] = back[1];
            cube_back[0] = down[1];
            cube_down[1] = cube_front[1];
            cube_front[1] = temp[0];
            cube_up[3] = back[0];
            cube_back[2] = down[0];
            cube_down[3] = cube_front[3];
            cube_front[3] = temp[1];
            //rotate left
            right_rotate(false);
        }
    }

    public void twist_right(int y){
        if(y == 0){
            String[] temp = {cube_right[0], cube_right[1]};
            cube_right[0] = cube_front[0];
            cube_front[0] = cube_left[0];
            cube_left[0] = cube_back[0];
            cube_back[0] = temp[0];
            cube_right[1] = cube_front[1];
            cube_front[1] = cube_left[1];
            cube_left[1] = cube_back[1];
            cube_back[1] = temp[1];
            //rotate left
            up_rotate(true);
        }
        if(y == 1){
            String[] temp = {cube_right[2], cube_right[3]};
            cube_right[2] = cube_front[2];
            cube_front[2] = cube_left[2];
            cube_left[2] = cube_back[2];
            cube_back[2] = temp[0];
            cube_right[3] = cube_front[3];
            cube_front[3] = cube_left[3];
            cube_left[3] = cube_back[3];
            cube_back[3] = temp[1];
            //rotate left
            down_rotate(true);
        }
    }

    public void twist_left(int y){
        if(y == 0){
            String[] temp = {cube_left[0], cube_left[1]};
            cube_left[0] = cube_front[0];
            cube_front[0] = cube_right[0];
            cube_right[0] = cube_back[0];
            cube_back[0] = temp[0];
            cube_left[1] = cube_front[1];
            cube_front[1] = cube_right[1];
            cube_right[1] = cube_back[1];
            cube_back[1] = temp[1];
            //rotate left
            up_rotate(false);
        }
        if(y == 1){
            String[] temp = {cube_left[2], cube_left[3]};
            cube_left[2] = cube_front[2];
            cube_front[2] = cube_right[2];
            cube_right[2] = cube_back[2];
            cube_back[2] = temp[0];
            cube_left[3] = cube_front[3];
            cube_front[3] = cube_right[3];
            cube_right[3] = cube_back[3];
            cube_back[3] = temp[1];
            //rotate left
            down_rotate(false);
        }
    }


    public int[] get_state(){
        // up front left down back right
        int[] state = {0,0,0,0,0,0};
        //Up
        String temp = cube_up[0];
        if(cube_up[1] == temp &&cube_up[2] == temp && cube_up[3] == temp){
            state[0] =1;
        }
        //Front
        temp = cube_front[0];
        if(cube_front[1] == temp &&cube_front[2] == temp && cube_front[3] == temp){
            state[1] =1;
        }
        //left
        temp = cube_left[0];
        if(cube_left[1] == temp &&cube_left[2] == temp && cube_left[3] == temp){
            state[2] =1;
        }
        //down
        temp = cube_down[0];
        if(cube_down[1] == temp &&cube_down[2] == temp && cube_down[3] == temp){
            state[3] =1;
        }
        //back
        temp = cube_back[0];
        if(cube_back[1] == temp &&cube_back[2] == temp && cube_back[3] == temp){
            state[4] =1;
        }
        //right
        temp = cube_right[0];
        if(cube_right[1] == temp &&cube_right[2] == temp && cube_right[3] == temp){
            state[5] =1;
        }
        return state;
    }

    private void left_rotate(boolean dir){
        if(dir){
            String cube_temp = cube_left[0];
            cube_left[0] = cube_left[1];
            cube_left[1] = cube_left[3];
            cube_left[3] = cube_left[2];
            cube_left[2] = cube_temp;
        }
        else{
            String cube_temp = cube_left[0];
            cube_left[0] = cube_left[2];
            cube_left[2] = cube_left[3];
            cube_left[3] = cube_left[1];
            cube_left[1] = cube_temp;
        }
    }

    private void right_rotate(boolean dir){
        if(dir){
            String cube_temp = cube_right[0];
            cube_right[0] = cube_right[2];
            cube_right[2] = cube_right[3];
            cube_right[3] = cube_right[1];
            cube_right[1] = cube_temp;
        }
        else{

            String cube_temp = cube_right[0];
            cube_right[0] = cube_right[1];
            cube_right[1] = cube_right[3];
            cube_right[3] = cube_right[2];
            cube_right[2] = cube_temp;
        }
    }

    private void up_rotate(boolean dir){
        if(dir){
            String cube_temp = cube_up[0];
            cube_up[0] = cube_up[1];
            cube_up[1] = cube_up[3];
            cube_up[3] = cube_up[2];
            cube_up[2] = cube_temp;
        }
        else{
            String cube_temp = cube_up[0];
            cube_up[0] = cube_up[2];
            cube_up[2] = cube_up[3];
            cube_up[3] = cube_up[1];
            cube_up[1] = cube_temp;
        }
    }

    private void down_rotate(boolean dir){
        if(dir){
            String cube_temp = cube_down[0];
            cube_down[0] = cube_down[2];
            cube_down[2] = cube_down[3];
            cube_down[3] = cube_down[1];
            cube_down[1] = cube_temp;
        }
        else{

            String cube_temp = cube_down[0];
            cube_down[0] = cube_down[1];
            cube_down[1] = cube_down[3];
            cube_down[3] = cube_down[2];
            cube_down[2] = cube_temp;
        }
    }

    public String[] getCube_up() {
        return cube_up;
    }

    public String[] getCube_down() {
        return cube_down;
    }

    public String[] getCube_front() {
        return cube_front;
    }

    public String[] getCube_back() {
        return cube_back;
    }

    public String[] getCube_left() {
        return cube_left;
    }

    public String[] getCube_right() {
        return cube_right;
    }


}
