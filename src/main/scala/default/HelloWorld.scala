package default

class HelloWorld {
  def greeting:String = {
    return "Hello World"
  }
}

object HelloWorld extends App{
  println(new HelloWorld().greeting)
}