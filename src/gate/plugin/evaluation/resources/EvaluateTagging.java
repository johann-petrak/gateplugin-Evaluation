package gate.plugin.evaluation.resources;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Controller;
import gate.Resource;
import gate.annotation.AnnotationSetImpl;
import gate.annotation.ImmutableAnnotationSetImpl;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.plugin.evaluation.api.AnnotationDifferTagging;
import gate.plugin.evaluation.api.ByThEvalStatsTagging;
import gate.plugin.evaluation.api.EvalStatsTagging;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO(!!!): Add a java class for holding counts or instances of pairings between the reference set
// and the response set so we can calculate p-values for the SingleResponse accuracy analysis. 
// This could be used to get p-values for the McNemar test and the paired t-test.

/**
 *
 * @author Johann Petrak
 */
@CreoleResource(name = "EvaluatePRF",
        comment = "Calculate P/R/F evalutation measures for documents")
public class EvaluateTagging extends AbstractLanguageAnalyser
  implements ControllerAwarePR  {

  ///////////////////
  /// PR PARAMETERS 
  ///////////////////
  
  
  private String keyASName;
  @CreoleParameter (comment="The name of the annotation set that contains the target/key annotations (gold standard)", defaultValue="Key")
  @RunTime
  public void setKeyASName(String name) { keyASName = name; }
  public String getKeyASName() { return keyASName; }
  
  private String responseASName;
  @CreoleParameter (comment="The name of the annotation set that contains the response annotations",defaultValue ="Response")
  @RunTime
  public void setResponseASName(String name) { responseASName = name; }
  public String getResponseASName() { return responseASName; }
  
  private String referenceASName;
  @CreoleParameter (comment="The name of the annotation set that contains the reference/old response annotations. Empty means no reference set.")
  @Optional
  @RunTime
  public void setReferenceASName(String name) { referenceASName = name; }
  public String getReferenceASName() { return referenceASName; }
  
  private String containingASNameAndType;
  @CreoleParameter (comment="The name of the restricting annotation set and the name of the type in the form asname:typename")
  @Optional
  @RunTime
  public void setContainingASNameAndType(String name) { containingASNameAndType = name; }
  public String getContainingASNameAndType() { return containingASNameAndType; }
  
  private ContainmentType containmentType;
  @CreoleParameter (comment="How the responses are restricted to the annotations of the containingASNameAndType",defaultValue="OVERLAPPING")
  @Optional
  @RunTime
  public void setContainmentType(ContainmentType ct) { ct = containmentType; }
  public ContainmentType getContainmentType() { return containmentType; }
  
  private String annotationType;
  @CreoleParameter (comment="The annotation type to use for evaluations",defaultValue="Mention")
  @RunTime
  public void setAnnotationType(String name) { annotationType = name; }
  public String getAnnotationType() { return annotationType; }
  
  private List<String> featureNames;
  @CreoleParameter (comment="A list of feature names to use for comparison, can be empty. First is used as the id feature, if necessary.")
  @RunTime
  @Optional
  public void setFeatureNames(List<String> names) { featureNames = names; }
  public List<String> getFeatureNames() { return featureNames; }
  
  private List<String> byValueFeatureNames;
  @CreoleParameter (comment="A list of feature names to use for breaking up the evaluation (NOT IMPLEMENTED YET)")
  @RunTime
  @Optional
  public void setByValueFeatureNames(List<String> names) { byValueFeatureNames = names; }
  public List<String> getByValueFeatureNames() { return byValueFeatureNames; }
  
  private String scoreFeatureName;
  @CreoleParameter (comment="The name of the feature which contains a numeric score or confidence. If specified will generated P/R curve.")
  @Optional
  @RunTime
  public void setScoreFeatureName(String name) { scoreFeatureName = name; }
  public String getScoreFeatureName() { return scoreFeatureName; }
  
  private String outputASName;
  @CreoleParameter (comment="The name of the annotation set for creating descriptive annotations. If empty, no annotations are created.")
  @Optional
  @RunTime
  public void setOutputASName(String name) { outputASName = name; }
  public String getOutputASName() { return outputASName; }
  
  public String featureNameNilCluster;
  @CreoleParameter(comment = "", defaultValue = "")
  @RunTime
  @Optional  
  public void setFeatureNameNilCluster(String value) { featureNameNilCluster = value; }
  public String getFeatureNameNilCluster() { return featureNameNilCluster; }
  
  public NilTreatment nilTreatment;
  @CreoleParameter(comment="",defaultValue="NO_NILS")
  @RunTime
  @Optional  
  public void setNilTreatMent(NilTreatment value) { nilTreatment = value; }
  public NilTreatment getNilTreatMent() { return nilTreatment; }
     
  public String nilValue;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setNilValue(String value) { nilValue = value; }
  public String getNilValue() { return nilValue; }
     
  public URL outputDirectoryUrl;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setOutputDirectoryUrl(URL value) { outputDirectoryUrl = value; }
  public URL getOutputDirectoryUrl() { return outputDirectoryUrl; }
     
  public String evaluationId;
  @CreoleParameter(comment="",defaultValue="")
  @RunTime
  @Optional  
  public void setEvaluationId(String value) { evaluationId = value; }
  public String getEvaluationId() { return evaluationId == null ? "" : evaluationId; }
     
  public ByThEvalStatsTagging.WhichThresholds whichThresholds;
  @CreoleParameter(comment="",defaultValue="USE_ALL")
  @RunTime
  @Optional  
  public void setWhichThresholds(ByThEvalStatsTagging.WhichThresholds value) { whichThresholds = value; }
  public ByThEvalStatsTagging.WhichThresholds getWhichThresholds() { return whichThresholds; }
     
  
  //////////////////// 
  // PR METHODS 
  ///////////////////

  @Override
  public Resource init() {    
    return this;
  }

  
  @Override
  public void reInit() {
    init();
  }
  
  @Override
  public void cleanup() {
    //
  }
  
  // fields shared between the execute method and the methods for initializing and finalization
  protected EvalStatsTagging allDocumentsStats;
  protected EvalStatsTagging allDocumentsReferenceStats = null;
  
  // This will be initialized at the start of the run and be incremented in the AnnotationDifferTagging
  // for each document.
  protected ByThEvalStatsTagging evalStatsByThreshold;
  
  
  @Override
  public void execute() {
    
    // Per document initialization
    
    // Prepare the annotation sets
    AnnotationSet keySet = document.getAnnotations(getStringOrElse(getKeyASName(), "")).get(getAnnotationType());
    AnnotationSet responseSet = document.getAnnotations(getStringOrElse(getResponseASName(), "")).get(getAnnotationType());
    AnnotationSet referenceSet = null;
    if(!getStringOrElse(getReferenceASName(), "").isEmpty()) {
      referenceSet = document.getAnnotations(getStringOrElse(getReferenceASName(), "")).get(getAnnotationType());
    }
    AnnotationSet containingSet = null;
    String containingSetName = "";
    String containingType = "";
    if(!getStringOrElse(getContainingASNameAndType(),"").isEmpty()) {
      String[] setAndType = getContainingASNameAndType().split(":",2);
      if(setAndType.length != 2 || setAndType[0].isEmpty() || setAndType[1].isEmpty()) {
        throw new GateRuntimeException("Runtime Parameter continingASAndName not of the form setname:typename");
      }      
      containingSetName = setAndType[0];
      containingType = setAndType[1];
      containingSet = document.getAnnotations(setAndType[0]).get(setAndType[1]);
      // now filter the keys and responses. If the containing set/type is the same as the key set/type,
      // do not filter the keys.
      ContainmentType ct = containmentType;
      if(ct == null) ct = ContainmentType.OVERLAPPING;
      responseSet = selectOverlappingBy(responseSet,containingSet,ct);
      if(containingSetName.equals(keyASName) && containingType.equals(annotationType)) {
        // no need to do anything for the key set
      } else {
        keySet = selectOverlappingBy(keySet,containingSet,ct);
      }
      // if we have a reference set, we need to apply the same filtering to that one too
      if(referenceSet != null) {
        referenceSet = selectOverlappingBy(referenceSet,containingSet,ct);
      }
    } // have a containing set and type
    
    // Now depending on the NIL processing strategy, do something with those annotations which 
    // are identified as nil in the key and response sets.
    // NO_NILS: do nothing, all keys and responses are taken as is. If there are special NIL
    //   values in the key set, the responses must match them like any other value. Parameter nilValue
    //   is ignored here.
    // NIL_IS_ABSENT:
    //   In this case a missing response for a NIL key is treated as a correct match. 
    //   A NIL response that is not coextensive is treated as a partial correct match.
    //   A response that is not a nil is treated as a spurious response. Parameter nilValue is 
    //   used to know which keys and responses are nils. 
    //   NOTE: this needs special processing in the annotation differ, so we need to adapt/change the
    //   annotation differ for that!
    // NIL_CLUSTERS: 
    //   In this case, a missing response does not equal a key nil, because we need to provide
    //   a label to be correct. Parameter nilValue is used so we know which keys and responses 
    //   are nils.
    //   We match all non-NILs in the usual way, ignoring all the NILS both in the key and 
    //   response sets. We accumulate all the NIL annotations over all documents and after all 
    //   documents have been processed, we try to find an optimal assignment between them, based
    //   on the NIL labels. 
    // TODO!!!
    
    AnnotationDifferTagging docDiffer = new AnnotationDifferTagging(
            keySet,
            responseSet,
            featureNames,
            scoreFeatureName,
            evalStatsByThreshold           
    );
    allDocumentsStats.add(docDiffer.getEvalPRFStats());
    
    // Now if we have parameters to record the matchings, get the information from the docDiffer
    // and create the apropriate annotations.
    AnnotationSet outputAnnotationSet = null;
    if(getOutputASName() != null && !getOutputASName().isEmpty()) {
      outputAnnotationSet = document.getAnnotations(getOutputASName());
      docDiffer.addIndicatorAnnotations(outputAnnotationSet);
    }
    
    // If we have a reference set, also calculate the stats for the reference set
    if(referenceSet != null) {
      AnnotationDifferTagging docRefDiffer = new AnnotationDifferTagging(
              keySet,
              referenceSet,
              featureNames,
              scoreFeatureName,
              evalStatsByThreshold
      );
      allDocumentsReferenceStats.add(docDiffer.getEvalPRFStats());
      // if we need to record the matchings, also add the annotations for how things changed
      // between the reference set and the response set.
      if(outputAnnotationSet != null) {
        docRefDiffer.addIndicatorAnnotations(outputAnnotationSet);
        // Now add also the annotations that indicate the changes between the reference set and
        // the response set
        AnnotationDifferTagging.addChangeIndicatorAnnotations(outputAnnotationSet, docDiffer, docRefDiffer);
      }
      
      // TODO: increment the overall count of how things changed
    }
    
    
  }
  
  
  
  ////////////////////////////////////////////
  /// HELPER METHODS
  ////////////////////////////////////////////
  
  private void checkRequiredArguments() {
    if(getAnnotationType() == null || getAnnotationType().isEmpty()) {
      throw new GateRuntimeException("Runtime parameter annotationType must not be empty");
    }
  }
  private void initializeForRunning() {
    // Check if all required parameters have been set
    checkRequiredArguments();
    
    allDocumentsStats = new EvalStatsTagging();
    allDocumentsReferenceStats = new EvalStatsTagging();
    
    // avoid NPEs later
    if(featureNames == null) {
      featureNames = new ArrayList<String>();
    }
    // convert the feature list into a set
    Set<String> featureNameSet = new HashSet<String>();
    featureNameSet.addAll(featureNames);
    
    // check if we have duplicate entries in the featureNames
    if(featureNameSet.size() != featureNames.size()) {
      throw new GateRuntimeException("Duplicate feature in the feature name list");
    }
    
    if(getScoreFeatureName() != null && !getScoreFeatureName().isEmpty()) {
      evalStatsByThreshold = new ByThEvalStatsTagging(getWhichThresholds());
    }
    
    if(!getStringOrElse(getReferenceASName(), "").isEmpty()) {
      allDocumentsReferenceStats = new EvalStatsTagging();
    }
    
    if(getContainmentType() == null) {
      containmentType = ContainmentType.OVERLAPPING;
    }
    if(getNilTreatMent() == null) {
      nilTreatment = NilTreatment.NO_NILS;
    }

  }
  
  
  private String getStringOrElse(String value, String elseValue) {
    if(value == null) return elseValue; else return value;
  }
  
  /**
   * Filter the annotations in the set toFilter and select only those which 
   * overlap with any annotation in set by.
   * 
   * @param toFilter
   * @param by
   * @return 
   */
  private AnnotationSet selectOverlappingBy(AnnotationSet toFilterSet, AnnotationSet bySet, ContainmentType how) {
    if(toFilterSet.isEmpty()) return toFilterSet;
    if(bySet.isEmpty()) return AnnotationSetImpl.emptyAnnotationSet;
    Set<Annotation> selected = new HashSet<Annotation>();
    for(Annotation byAnn : bySet) {
      AnnotationSet tmp = null;
      if(how == ContainmentType.OVERLAPPING) {
        tmp = gate.Utils.getOverlappingAnnotations(toFilterSet, byAnn);
      } else if(how == ContainmentType.CONTAINING) {
        tmp = gate.Utils.getContainedAnnotations(toFilterSet, byAnn);          
      } else if(how == ContainmentType.COEXTENSIVE) {
        tmp = gate.Utils.getCoextensiveAnnotations(toFilterSet, byAnn);        
      } else {
        throw new GateRuntimeException("Odd ContainmentType parameter value: "+how);
      }
      selected.addAll(tmp);
    }
    return new ImmutableAnnotationSetImpl(document, selected);    
  }
  
  public void outputDefaultResults() {
    System.out.println(allDocumentsStats);
    if(!getStringOrElse(getReferenceASName(), "").isEmpty()) {
      System.out.println(allDocumentsReferenceStats);
    }
    
    // TODO: refactor this code into a method that returns a handle for writing to some file e.g. "byThresholds"
    // FileOutputStream os = getOutputStream("byThresholds") // no exception but may log an error and return null
    FileOutputStream os = null;
    if(outputDirectoryUrl != null) {
      File outFile = new File(gate.util.Files.fileFromURL(outputDirectoryUrl),getEvaluationId()+"byThresholds.tsv");
      try {
        os = new FileOutputStream(gate.util.Files.fileFromURL(outputDirectoryUrl));
      } catch (FileNotFoundException ex) {
        System.err.println("Could not open output file "+outFile+" for writing, writing to the log only");
      }
    }
    if(evalStatsByThreshold != null) {
      System.out.println("DEBUG: map has entries: "+evalStatsByThreshold.size());
      Double th = evalStatsByThreshold.firstKey();
      double highestPrecisionSoFarStrict = 0.0; // used for "interpolated precision"      
      double highestPrecisionSoFarLenient = 0.0; // used for "interpolated precision"      
      // TODO: output the header row
      System.out.println(EvalStatsTagging.getTSVHeaders());
      while(th != null) {
        // get the stats for that th
        EvalStatsTagging es = evalStatsByThreshold.get(th);
        // calculate the interpolated precision values for this row
        highestPrecisionSoFarStrict = Math.max(highestPrecisionSoFarStrict, es.getPrecisionStrict());
        highestPrecisionSoFarLenient = Math.max(highestPrecisionSoFarLenient, es.getPrecisionLenient());
        String line = es.getTSVLine();
        System.out.println(line);
        // get the next higher threshold
        th = evalStatsByThreshold.higherKey(th);
      }
      // TODO: output a row for everything above the highest seen score, which mean:
      // precision = 1.0, recall = 0.0, responses = 0, correct = 0 and incorrect = 0
      
      // TODO: consider additional measure to calculate, e.g. area under the PRC (APRC)
    }

  }
  
  
  ////////////////////////////////////////////
  /// CONTROLLER AWARE PR methods
  ////////////////////////////////////////////
  
  @Override
  public void controllerExecutionStarted(Controller cntrlr) throws ExecutionException {
    initializeForRunning();
  }

  @Override
  public void controllerExecutionFinished(Controller cntrlr) throws ExecutionException {
    outputDefaultResults();
  }

  @Override
  public void controllerExecutionAborted(Controller cntrlr, Throwable thrwbl) throws ExecutionException {
    System.err.println("Processing was aborted: "+thrwbl.getMessage());
    thrwbl.printStackTrace(System.err);
    System.err.println("Here are the summary stats for what was processed: ");
    outputDefaultResults();
  }
  
  
  public static enum ContainmentType {
      COEXTENSIVE,
      CONTAINING,
      OVERLAPPING
  }

  public static enum NilTreatment {
    NO_NILS,
    NIL_IS_ABSENT,
    NIL_CLUSTERS
  }
  
  
}