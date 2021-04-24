package com.crystalline.aether.services.utils;

public class Scrapbook {

    /**
     * Takes care of Chicken nests
     */
    public static class Barn{

    }

    /**
     * Handles eggs
     * */
    public static class Chicken{
        private Egg[] eggs;

        public Chicken(Egg... eggs_){ eggs = eggs_; }

        public void accept_signal(String message){ /* ... */ }
    }

    /**
     * Signals when its ready to hatch
     * */
    public static abstract class Egg{
        private final Chicken parent;
        public Egg(Chicken parent_){
            parent = parent_;
        }

        public void hatch(){
            parent.accept_signal("chirp chirp");
        }
    }

    /**
     * The resulting children to be handled
     * */
    public static class YellowChick extends Egg{
        public YellowChick(Chicken parent_){
            super(parent_);
        }
    }

    public static class BrownChick extends Egg{
        public BrownChick(Chicken parent_){
            super(parent_);
        }
    }

    public static class UglyDuckling extends Egg{
        public UglyDuckling(Chicken parent_){
            super(parent_);
        }
    }

    public static void main (String[] args){
        Chicken momma = new Chicken(
        /*  new YellowChick(),
	new BrownChick(),
	new UglyDuckling() How can these objects be initialized properly? */
        );
        System.out.println("Success!");
    }

}