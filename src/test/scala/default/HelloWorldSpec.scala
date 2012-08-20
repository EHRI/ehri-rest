package default

import org.specs2.mutable.Specification

class HelloWorldSpec extends Specification {

  "HelloWorld" should {
    "return 'Hello World'" in {
      new HelloWorld().greeting must be equalTo "Hello World"
    }
  }
}
  