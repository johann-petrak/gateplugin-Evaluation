doc = newD();
addA(doc,"Keys",0, 10,"M",featureMap("id","x"));
addA(doc,"Keys",20,30,"M",featureMap("id","x"));
addA(doc,"Keys",40,50,"M",featureMap("id","x"));
t = addA(doc,"Keys",60,70,"M",featureMap("id","x"));
addA(doc,"Resp",0,10,"M",featureMap("id","x","s","0.1"));
addA(doc,"Resp",20,30,"M",featureMap("id","x","s","0.2"));
addA(doc,"Resp",40,50,"M",featureMap("id","x","s","0.3"));
r = addA(doc,"Resp",60,70,"M",featureMap("id","x","s","0.4"));
bth = new ByThEvalStatsTagging(ByThEvalStatsTagging.WhichThresholds.USE_ALL);
ad = new AnnotationDifferTagging(t, r, FL_ID,"s",bth);
es = ad.getEvalStatsTagging();
