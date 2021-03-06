## Functions for the TaggingScore class.

#' Print a TaggingScore object.
#'
#' @param x the object to print
#' @param ...  additional parameters.
#' @export
print.TaggingScore <- function(x,...) {
  NextMethod("print")
  cat("TaggingScore, @maxFs: Ps/Rs/Fs Pl/Rl/Fl",
      paste(sep="/",p_d(x$precisionStrict),p_d(x$recallStrict),p_d(x$F1Strict)),
      paste(sep="/",p_d(x$precisionLenient),p_d(x$recallLenient),p_d(x$F1Lenient)),
      " at score th ",p_d(x$maxF1StrictScore),
      "\n")
  return(invisible(x))
}

#' Initialize an object of type Tagging Score
#'
#' @param x the object to initialize
#' @return the initialized object
initializeObject.TaggingScore <- function(x) {
  obj <- x$data
  obj <- dplyr::filter(obj, docName == "[doc:all:micro]")
  obj <- dplyr::filter(obj, !is.infinite(threshold))
  x$data = obj
  ## find the row with the maximum strict f value
  i=which.max(obj$F1Strict)
  x$maxF1StrictScore = obj$threshold[i]
  row = obj[i,]
  x = add_list_to_list(x,row)
  return(x)
}

#' Plot an object of type TaggingScore
#'
#' @param show either "t" to show the threshold or "f" to show the f measure
plot.TaggingScore <- function(x, strict=TRUE, add.text="t", add.lines="l", ...) {
  ## TODO: find out what is the best way to
  ## show score/Fs/Fl at the point of maximum Fs/Fl?
  ## show the score/Fs/Fl for points with a specific score, or the nth point?
  if(strict) {
    r = x$data$recallStrict
    p = x$data$precisionStrict
    f = signif(x$data$F1Strict,digits=2)
    lx = "Recall Strict"
    ly = "Precision Strict"
  } else {
    r = x$data$recallLenient
    p = x$data$precisionLenient
    f = signif(x$data$F1Lenient,digits=2)
    lx = "Recall Lenient"
    ly = "Precision Lenient"
  }
  th=x$data$threshold
  plot(r,p,xlab=lx,ylab=ly,...)
  if(is.null(add.lines)) {
    ## do nothing
  } else if(add.lines=="l") {
    lines(r,p,type="l")
  } else {
    stop("Parameter add.lines must be either NULL, or 'l'")
  }
  if(is.null(add.text)) {
    ## do nothing
  } else if(add.text=="t") {
    text(r,p,labels=th,cex=0.7,pos=3)
  } else if(add.text=="f") {
    text(r,p,labels=f,cex=0.7,pos=3)
  } else {
    stop("Parameter add.text must be either NULL (do not add), 't' (threshold) or 'f' (F-measure)")
  }
  invisible()
}

## TODO: implement summary.TYPE and plot.TYPE
## Also, decide on a number of methods that make it easier to access important
## measures, e.g. m_fl(obj) or m_fs(beta=0.5) or m_cifsl(conf=0.90) or m_ciplu
## of m_cifsr() (for range) etc.?
## All of these could be vectorized, so if they get a vector of evaluation
## objects, the return a vector of measures?
## Or, a vectorized function measures(objs,measurenames,...) which could return
## a matrix or data.frame such that the rows correspond to the objects and
## the columns correspond to the measures. The clou would be that the measures
## vector could allow for the actual names but also for all kinds of short forms.
## If objs is a single object, it would return a list instead of a data frame (?)
## if objs is a single object and there is only one measure, it would only
## return a single value (a vector, but we could still have a name for the value)
