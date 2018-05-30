# Rajit's notes

## Assumptions

* Phone number format is always ###-###-###
* Durations don't need to be validated/sanitised (e.g. values will never be negative)
* Report can use HALF_EVEN rounding and print costs to 2 decimal places
* Only needs to read `calls.log` in `src/main/resources` (e.g. hard-coded, not parameterised)
* Phone numbers can be compared without being cleaned up and don't require normalisation (e.g. don't need to trim whitespace)
* Calls of exactly 3 minutes should be charged at £0.0003/second
* Should ignore lines that cause errors and continue with report
* Should silently ignore blank lines

## Usage

Either run without arguments (to read from `./src/main/resources/calls.log`):

```
$ sbt run
```

or specify file to read:

```
$ sbt run -- /path/to/file/calls.log
```

_Original task instructions below >_

# Phone Company

Each day at The Phone Company a batch job puts all the customer calls for the previous day into a single log file of:

`'customer id','phone number called','call duration'`

For a customer the cost of a call under 3 minutes is 0.05p/sec, over 3 minutes it is 0.03p/sec. However, there is a promotion on and the calls made to the phone number with the greatest total cost is removed from the customer's bill.

## Task

Write a program that when run will parse the `calls.log` file and print out the total cost of calls for the day for each customer. You can use any libraries you wish to.

