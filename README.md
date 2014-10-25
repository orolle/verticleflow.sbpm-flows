# PoC: Execute a business process with vertigo

This project is a proof of concept. It proofs the ability of vertigo to execute business processes in a scalable way.
Therefore a simple order process is implemented: A customer buys product1 or product2 and an employee has to accept or
reject the order.

This project mixes flow based programming (FBP) with subject oriented business process management (SBPM) 
(http://en.wikipedia.org/wiki/Flow-based_programming ; http://en.wikipedia.org/wiki/Subject-oriented_business_process_management)

FBP is procedural programming language which applies predicates (methods) onto objects (data). 
This is enough for most pure-software projects, but as software integrates into our lives it is incomplete with respect to
whom (subject) executes the predicates onto objects. Here objects are not only data but physical objects too - like the 
author of this project did today as his cooking app ordered him to cut vegetables and put them into the pot. From modeling 
perspective this is similar to order my computer to calculate the sum of 1 and 2.

Therefore this project introduces subjects to FBP. The business process above has 2 subjects: Customer and Employee.
Subjects are always stateful and interact with each other and its environment (like a roboter or a cook). The PoC uses components 
as subjects which have ports to receive and send (business) objects to each other based on a network topology.

Further more this project has WebRequest and WebResponse components which are corresponding to a "WebService" subject which 
acts on behalf of your web browser, if you connect onto localhost:8080. 
Here you see that components and subjects might not be 1 to 1 related. 

If WebRequest receives a http request, the requets is forwared to a special component named "ScopeSelector" which
instantiates business processes and uses the request as input for the process. The business process acts on the 
request and the result is forwared back through the ScopeSelector to the WebResponse which sends the result to the browser.

The ScopeSelector creates new business processes instances which are vertigo networks. The ScopeSelector translates
between a FBP and SBPM world. In FBP web request are data, which can be translated to SBPM business objects. These business
objects changes the state of a single or multipe subjects, similar an event changes the state of a final state machine. 
As response to these state changes, the process generates a html output which is routed to the browser.

As mentioned above the business process is a vertigo network. The Customer subject in the class sbpm.order.Customer has 3 states:
before customer orders, after customer ordered and order accepted / denied by Employee. Its state changes if a web request with 
an order is send from ScopeSelector to the Customer or the Employee sended the Customer an accept or deny order. The interaction
of the Customer and the Employee is modelled with a vertigo network. 
The ScopeSelector is a special kind of interaction, similar to a puppet master and sends the business object via a special 
web inport to the Customer or Employee. (This behavior makes building web based sbpm easy to implement.) 
The Customer / Employee replies on the web outport on a web request with an html page representing their current state.


# Build and Run

mvn package ; cd target/; vertx runzip verticleflow.sbpm-flows-PoC-0.1-mod.zip ; cd ..

Go to localhost:8080 

Click on customer (to interact with the order process from the customer view) or employee (to interact with the process from an
employee view).


# Questions or suggestions

Post them onto the vertigo mailing list under https://groups.google.com/forum/#!forum/vertx-vertigo
I will answer them as soon as possible.
