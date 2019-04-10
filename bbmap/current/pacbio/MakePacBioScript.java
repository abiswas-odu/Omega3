package pacbio;

import java.io.File;

import dna.Data;
import fileIO.ReadWrite;
import fileIO.TextFile;
import shared.Tools;

/**
 * @author Brian Bushnell
 * @date Oct 2, 2012
 *
 */
public class MakePacBioScript {
	
	/**
		Be sure to replace:
		@BUILDNUM with a number
		@DIRTY_INPUT with the PacBio file
		@CLEAN_INPUT_1 with the Illumina file
		@ORGANISM with the name of the organism (or whatever)
		@NUMSLOTS with the number of slots requested
		@TARGET_SIZE with an estimate of the genome size, in bases.  Examples:  160000000 or 160m or 0.16g are equivalent.
		@RAM with e.g. Xmx31g
		@MAXRAM with e.g. Xmx220g
		@SCRIPT with the output file, e.g. run.sh
		@MERGEREF with a list of reference files, e.g. chrom1.fa,chrom2.fa,chrom3.fa
		@MERGEDIRTY with a list of dirty files, e.g. subreads1.fa,subreads2.fa,subreads3.fa
		@MERGECLEAN with a list of clean files, e.g. illumina1.fq,illumina2.fq,illumina3.fq
		@EXTRA with extra files for Illumina error correction.  e.g. extra=a.fq,b.fq,c.fq
		
		Optional:
		@MAXREADS with the max number of clean reads to use in phase 1 (the slowest phase)
		@REFERENCE with a reference file (optional)
		@REFBUILD with a number
	 */
	public static void main(String[] args){
		
		if(args==null || args.length<1){
			System.out.println("\nThis program generates a script for error-correcting PacBio reads using Illumina reads.\nSample command line:\n");
//			System.out.println("java -ea -Xmx64m"+(Data.WINDOWS ? "" : " -cp "+Data.ROOT)+" jgi.MakePacBioScript " +
//					"dirty=subreads.fa clean=illumina.fq ref=ecoliRef.fa name=ecoli " +
//					"out=run.sh template="+(Data.WINDOWS ? "" : "/house/homedirs/b/bushnell/template/")+"cleanPacbioTemplate.sh " +
//					"targetsize=5.4m threads=24 ram=31 maxram=100 noderam=256 build=-1 refbuild=-1 maxreads=-1");
//			System.out.println("java -ea -Xmx64m"+(Data.WINDOWS ? "" : " -cp "+Data.ROOT)+" jgi.MakePacBioScript " +
//					"dirty=subreads.fa clean=illumina.fq ref=ecoliRef.fa name=ecoli " +
//					"out=run.sh template="+(Data.WINDOWS ? "" : "/house/homedirs/b/bushnell/template/")+"cleanPacbioTemplate.sh " +
//					"targetsize=5.4m threads=24 noderam=256");
//			System.out.println("\n\nOr to be concise:");
			System.out.println("java -ea -Xmx64m"+(Data.WINDOWS ? "" : " -cp "+Data.ROOT())+" jgi.MakePacBioScript " +
					"d=subreads.fa c=illumina.fq tpl=template.sh ts=5.4m t=24 nm=256");
			System.out.println("\n\nInput files can optionally be comma-separated lists of files, and absolute pathing can be used.");
			System.out.println("All input files may be raw, gzipped, or bzipped as long as they have the correct file extension.");
			System.out.println();
			System.out.println("\n*****    Required Parameters   *****\n");
			System.out.println("d=, dirty=       \tPath to dirty (PacBio) reads.  May be comma-delimited for multiple files.");
			System.out.println("c=, clean=       \tPath to clean (Illumina) reads.  May be comma-delimited for multiple files.");
			System.out.println("t=, threads=     \tNumber of threads.  Should equal the number of qsub slots or cores on the target machine.");
			System.out.println("nm=, nodemem=    \tPhysical memory (RAM) of target machine, in gigabytes.");
			System.out.println("ts=, targetsize= \tEstimated size of target genome, in bases (k, m, or g may be used).  Optional ONLY if a reference is supplied.");
			System.out.println("\n*****    Optional Parameters   *****\n");
			System.out.println("tpl=, template=  \tPath to template for this script.  Default is /house/homedirs/b/bushnell/template/cleanPacbioTemplate_ecc.sh");
			System.out.println("mode=            \tCan be specified instead of 'template='.  Values are 'pacbio', 'assembly', or 'ccs'");
			System.out.println("sort=            \tTrue or false.  Determines whether clean reads are sorted (alphabetically) and duplicates are removed.");
			System.out.println("r=, ref=         \tPath to reference fasta.  May be comma-delimited for multiple files.");
			System.out.println("o=, out=         \tName of output script.  Default is 'run.sh'.");
			System.out.println("name=            \tName of organism.  Default is 'organism'.");
			System.out.println("h=, hours=       \tTime limit (in hours) for autogenerated qsub command line.");
			System.out.println("m=,mem=          \tAmount of heap memory for Java to use.  Default is 31g; must be at least 10x number input PacBio bases." +
						"\n                 \tNote! Two steps, Illumina error correction and site stacking, will ignore this and use all physical memory.");
			System.out.println("b=,build=        \tPrefix for index build number.  Default is 2, yielding successively improved builds 2, 200, 201, 202, ... 208");
			System.out.println("rb=,refbuild=    \tReference build number.  Default is 1.");
			System.out.println("cp=,classpath=   \tClasspath to the program.  If unspecified, will be autodetected as "+
					(Data.WINDOWS ? "/house/homedirs/b/bushnell/beta19/" : Data.ROOT()));
//			r=ref.fa o=run.sh
			System.exit(0);
		}
		
		String dirty=null;
		String clean=null;
		String name="organism";
		String targetsize=null;
		String ref=null;
		String template=null;
		String output="run.sh";
		String extra="";
		String classpath=(Data.WINDOWS ? "/house/homedirs/b/bushnell/beta19/" : Data.ROOT());
		String sort_in="";
		String sorted="sorted_topo#.txt.gz";
		String sorted_out="sorted_topo1.txt.gz";
		
		String mergeref=null;
		String mergedirty=null;
		String mergeclean=null;
		
		String qsub=null;
		
		String cleanecc="@ORGANISM_ecc_1.txt.gz";
		String cleanbadecc="@ORGANISM_ecc_1_BAD.txt.gz";
		String cleanallecc="@ORGANISM_ecc_1_ALL.txt.gz";
		
		String mode="pacbio";
		
		int build=2;
		int threads=24;
		int ram=31;
		int maxram=-1;
		int refbuild=-1;
		int noderam=-1;
		long maxReads=-1;
		int runtime=499;
		
		boolean ecc=false;
		boolean sort=true;
		
		for(int i=0; i<args.length; i++){
			final String arg=args[i];
			final String[] split=arg.split("=");
			if(split.length!=2){
				System.out.println("Wrong number of arguments for variable "+split[0]);
				System.exit(0);
			}
			String a=split[0].toLowerCase();
			String b=split[1];
			if(b.equalsIgnoreCase("null")){b=null;}
			
			if(a.equals("threads") || a.startsWith("slots") || a.equals("t")){
				threads=Integer.parseInt(b);
			}else if(a.equals("mode")){
				mode=b;
			}else if(a.startsWith("reads") || a.startsWith("maxreads") || a.equals("rd")){
				maxReads=Tools.parseKMG(b);
			}else if(a.startsWith("build") || a.startsWith("genome") || a.equals("b")){
				build=Integer.parseInt(b);
				String s=Data.chromFname(1, build);
				if((new File(s)).exists()){System.out.println("Warning! Genome build "+b+" already exists at "+s);}
			}else if(a.startsWith("refbuild") || a.startsWith("refgenome") || a.equals("rb")){
				refbuild=Integer.parseInt(b);
				String s=Data.chromFname(1, refbuild);
				if((new File(s)).exists()){System.out.println("Warning! Genome build "+b+" already exists at "+s);}
			}else if(a.startsWith("ram") || a.startsWith("mem") || a.equals("m")){
				ram=Integer.parseInt(b.toLowerCase().replaceAll("g", ""));
			}else if(a.startsWith("maxram") || a.startsWith("maxmem") || a.equals("mm")){
				maxram=Integer.parseInt(b.toLowerCase().replaceAll("g", ""));
			}else if(a.startsWith("noderam") || a.startsWith("nodemem") || a.equals("nm")){
				if(b!=null){noderam=Integer.parseInt(b.toLowerCase().replaceAll("g", ""));}
			}else if(a.equals("runtime") || a.equals("hours") || a.equals("time") || a.equals("h")){
				runtime=Integer.parseInt(b.toLowerCase().replaceAll("h", ""));
			}else if(a.startsWith("dirty") || a.startsWith("pacbio") || a.equals("d")){
				dirty=b;
				if(dirty.contains(",")){
					mergedirty=dirty;
					dirty="concatenatedDirtyFiles.fa.gz";
					if((new File(dirty)).exists()){System.out.println("Warning! file already exists: "+dirty);}
				}else{
					if(!(new File(b)).exists()){System.out.println("Warning! No such file: "+b);}
				}
			}else if(a.startsWith("clean") || a.startsWith("illumina") || a.equals("c")){
				clean=b;
				
				if(clean.contains(",")){
					String ext="fq";
					if(clean.contains(".fasta,") || clean.contains(".fa,") || clean.contains(".fasta.gz,") || clean.contains(".fa.gz,")){ext="fa";}
					else if(clean.contains(".txt,") || clean.contains(".txt.gz,")){ext="txt";}
						
					mergeclean=clean;
					clean="concatenatedCleanFiles."+ext+".gz";
					if((new File(clean)).exists()){System.out.println("Warning! file already exists: "+clean);}
				}else{
					if(!(new File(b)).exists()){System.out.println("Warning! No such file: "+b);}
				}
			}else if(a.startsWith("name") || a.startsWith("organism")){
				name=b;
				if(name==null){name="organism";}
			}else if(a.startsWith("size") || a.startsWith("targetsize") || a.equals("ts")){
				targetsize=b;
			}else if(a.equals("path") || a.equals("classpath") || a.equals("cp")){
				classpath=b;
			}else if(a.startsWith("template") || a.equals("tpl")){
				if(b!=null){
					template=b;
					if(!(new File(b)).exists()){System.out.println("Warning! No such file: "+b);}
				}
			}else if(a.startsWith("extra") || a.equals("ex")){
				extra=("extra="+b);
				if(!b.contains(",") && !(new File(b)).exists()){System.out.println("Warning! No such file: "+b);}
			}else if(a.startsWith("ref") || a.equals("r")){
				ref=b;
				if(ref.contains(",")){
					mergeref=ref;
					ref="concatenatedReferenceFiles.fa.gz";
					if((new File(ref)).exists()){System.out.println("Warning! file already exists: "+ref);}
				}else{
					if(!(new File(b)).exists()){System.out.println("Warning! No such file: "+b);}
				}
			}else if(a.startsWith("out") || a.equals("o")){
				output=b;
				if((new File(b)).exists()){System.out.println("Warning! Outfile already exists: "+b);}
			}else if(a.startsWith("ecc")){
				ecc=Tools.parseBoolean(b);
			}else if(a.equals("sort")){
				sort=Tools.parseBoolean(b);
			}else{
				throw new RuntimeException("Unknown parameter "+args[i]);
			}
		}
		
		if(template==null){
			if(mode==null){mode="pacbio";}
			mode=mode.toLowerCase();
			if(mode.equals("pacbio") || mode.equals("pacbio_illumina")){
				template=(Data.WINDOWS ? "C:/workspace/prune/cleanPacbioTemplate_ecc.sh" : "/house/homedirs/b/bushnell/template/cleanPacbioTemplate_ecc_maxram.sh");
			}else if(mode.equals("assembly") || mode.equals("assembly_illumina")
					|| mode.equals("reference") || mode.equals("reference_illumina")){
				template=(Data.WINDOWS ? "C:/workspace/prune/correctReference.sh" : "/house/homedirs/b/bushnell/template/correctReference_maxram.sh");
			}else if(mode.equals("ccs") || mode.startsWith("ccs_")){
				throw new RuntimeException("TODO: Mode "+mode+" is unfinished.");
			}else if(mode.equals("pacbio_ccs") || mode.endsWith("_ccs")){
				throw new RuntimeException("TODO: Mode "+mode+" is unfinished.");
			}
		}
//		assert(false) : mode+", "+template;
		
		if(ecc){
			if(sort){
				sort_in=cleanecc;
				sorted_out=sorted.replaceFirst("#", "1");
			}else{
				sorted_out=sorted=clean;
			}
		}else{
			if(sort){
				sort_in=clean;
				sorted_out=sorted.replaceFirst("#", "1");
				cleanecc=sorted_out;
				cleanbadecc=sorted_out;
				cleanallecc=sorted_out;
			}else{
				sorted_out=sorted=clean;
				cleanecc=clean;
				cleanbadecc=clean;
				cleanallecc=clean;
			}
		}
		
		assert(threads>0);
		
		if(noderam<1){
			if(threads<9){noderam=144;}
			else if(threads<25){noderam=252;}//Changed due to crash at 217 GB on 24-core nodes.
			else if(threads<33){noderam=512;}
			else if(threads<41){noderam=1024;}
			else{noderam=2048;}
			System.out.println("Set noderam at "+noderam+"g");
		}
		
		String slotram;
		if(noderam%threads==0){slotram=(noderam/threads)+"G";}
		else{slotram=((noderam*990)/threads)+"M";}
		
		if(noderam>0){
			if(maxram<1){
				maxram=(int)(noderam*(noderam>256 ? 0.83 : 0.85f));
				System.out.println("Set maxram at "+maxram+"g");
			}
		}

		if(ram>maxram){
			ram=maxram;
			System.out.println("Set ram at "+maxram+"g");
		}
		
		if("auto".equalsIgnoreCase(targetsize) || (targetsize==null && ref!=null)){
			if(ref==null){throw new RuntimeException("Ref file must be specified for auto targetsize.");}
			File f=new File(ref);
			if(!f.exists()){throw new RuntimeException("Ref file must exist for auto targetsize.");}
			if(f.exists()){
				targetsize=""+new File(ref).length();
				if(ref.endsWith(".gz") || ref.endsWith(".gzip") || ref.endsWith(".zip") || ref.endsWith(".bz2")){
					TextFile tf=new TextFile(ref, false);
					long x=1;
					for(String s=tf.nextLine(); s!=null; s=tf.nextLine()){x+=s.length();}
					tf.close();
					targetsize=""+x;
				}
			}
		}
		
		if(ref!=null && refbuild<1){
			if(build==1){refbuild=2;}
			else{refbuild=1;}
		}

		if(dirty==null){throw new RuntimeException("No dirty file specified.");}
		if(clean==null){throw new RuntimeException("No clean file specified.");}
		if(targetsize==null){throw new RuntimeException("No targetsize specified.");}
		if(template==null){throw new RuntimeException("No template file specified.");}
		if(!new File(template).exists()){throw new RuntimeException("Template file "+template+" does not exist; please specify a different template.");}
		if(build==refbuild){throw new RuntimeException("Build id and ref build id must differ.");}
		if(build<1){throw new RuntimeException("No build id.");}
		if(ref!=null && refbuild<1){throw new RuntimeException("No ref build id.");}
		if(ref==null && refbuild>0 && !(new File(Data.chromFname(1, refbuild))).exists()){throw new RuntimeException("Ref build id specified, but no reference file.");}

		String[] lines;
		{
			TextFile tf=new TextFile(template, false);
			lines=tf.toStringLines();
		}


		StringBuilder sb=new StringBuilder();
		for(int i=0; i<lines.length; i++){
			String s=lines[i];
			
			boolean eccline=s.contains("?ecc?");
			boolean sortline=s.contains("?sort?");
			boolean refline=s.contains("?ref?");
			boolean mergeline=s.contains("?ref?");
			boolean optional=(!eccline && !sortline && !refline && !mergeline && s.startsWith("#?")); //Optional for some other reason
			
			if(eccline){
				s=s.replaceAll("\\?ecc\\?", "");
				while(ecc && s.startsWith("#")){s=s.substring(1);}
			}
			if(sortline){
				s=s.replaceAll("\\?sort\\?", "");
				while(sort && s.startsWith("#")){s=s.substring(1);}
			}
			if(refline){
				s=s.replaceAll("\\?ref\\?", "");
				while(refbuild>0 && s.startsWith("#")){s=s.substring(1);}
			}
			
			if(!s.startsWith("#")){
				if((eccline && !ecc) || (sortline && !sort) || (refline && refbuild<1)){s="#"+s;}
			}
			
			
			if(optional){
				optional=true;
				s=s.substring(2);
			}
			
			if((s.contains("@MAXRAM") && maxram>31) || (s.contains("@RAM") && ram>31)){
				s=s.replace("-XX:+UseCompressedOops ", "");
			}

			s=s.replace("@CLEAN_ECC_1", cleanecc);
			s=s.replace("@CLEAN_BAD_ECC_1", cleanbadecc);
			s=s.replace("@CLEAN_ALL_ECC_1", cleanallecc);
			s=s.replace("@SORT_IN", sort_in);
			s=s.replace("@SORTED_OUT", sorted_out);
			s=s.replace("@SORTED", sorted);
			
			s=s.replace("@SLOTRAM", slotram);
			s=s.replace("@BUILDNUM", ""+build);
			s=s.replace("@DIRTY_INPUT", dirty);
			s=s.replace("@CLEAN_INPUT_1", clean);
			s=s.replace("@ORGANISM", name);
			s=s.replace("@NUMSLOTS", ""+threads);
			s=s.replace("@TARGET_SIZE", targetsize);
			s=s.replace("@RAM", "-Xmx"+ram+"g");
			s=s.replace("@MAXRAM", "-Xmx"+maxram+"g");
			s=s.replace("@MAXREADS", ""+maxReads);
			s=s.replace("@SCRIPT", (output==null ? "run.sh" : output));
			s=s.replace("@EXTRA", extra);
			s=s.replace("@RUNTIME", ""+runtime);
			s=s.replace("@CLASSPATH", classpath);
			
			if(s.contains("@REFBUILD")){
				if(refbuild<1){
					s="#"+s;
				}else{
					s=s.replace("@REFBUILD", ""+refbuild);
				}
			}
			
			
			if(s.contains("@REFERENCE")){
				if(ref==null){
					s="#"+s;
				}else{
					s=s.replace("@REFERENCE", ref);
				}
			}
			
			if(s.contains("@MERGECLEAN")){
				if(mergeclean==null){
					s="#"+s;
				}else{
					s=s.replace("@MERGECLEAN", mergeclean);
				}
			}
			
			if(s.contains("@MERGEDIRTY")){
				if(mergedirty==null){
					s="#"+s;
				}else{
					s=s.replace("@MERGEDIRTY", mergedirty);
				}
			}
			
			if(s.contains("@MERGEREF")){
				if(mergeref==null){
					s="#"+s;
				}else{
					s=s.replace("@MERGEREF", mergeref);
				}
			}
			
			while(s.startsWith("##")){s=s.substring(1);}
			
			assert(s==null || s.length()<1 || s.startsWith("#") || !s.contains("@")) : s;
			
			if(s!=null && !s.startsWith("#//")){sb.append(s).append('\n');}
			
			if(qsub==null && s.contains("export task") && s.contains("qsub")){
				qsub=s;
			}
		}
		
		if(output==null){
			System.out.println(sb);
		}else{
			ReadWrite.writeString(sb, output, false);
			System.out.println("Wrote "+output);
			if(qsub!=null){
				while(qsub.startsWith("#")){qsub=qsub.substring(1);}
				System.out.println("The script can be executed on Genepool with the following command:\n\n"+qsub.trim());
			}
		}
		
		
	}
	

}
