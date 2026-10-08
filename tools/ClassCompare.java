import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Bytecode-level comparison of two sets of class files (e.g. the ORIGINAL jar's classes vs a decompile->recompile
 * of them). For every method it collects the SET of things the code touches: invoked methods, field reads/writes,
 * constants (so float-vs-double or wrong-literal decompiler slips show up), created/checked types and invokedynamic
 * sites. Methods whose sets differ are reported. Lambdas are matched by content, not by their numbered names.
 *
 * Needs a Java 24+ runtime (java.lang.classfile).   java ClassCompare.java <dirA> <dirB> [ClassNameFilter]
 */
public final class ClassCompare {
   static Map<String, Set<String>> fingerprints(Path classFile) throws IOException {
      ClassModel cm = ClassFile.of().parse(Files.readAllBytes(classFile));
      Map<String, Set<String>> out = new TreeMap<>();
      List<Set<String>> lambdas = new ArrayList<>();
      for (MethodModel mm : cm.methods()) {
         String name = mm.methodName().stringValue();
         Set<String> fp = new TreeSet<>();
         mm.code().ifPresent(code -> {
            for (CodeElement e : code) {
               switch (e) {
                  case InvokeInstruction i -> fp.add("I " + i.owner().asInternalName() + "." + i.name().stringValue() + i.type().stringValue());
                  case FieldInstruction f -> fp.add((f.opcode().name().startsWith("GET") ? "G " : "P ") + f.owner().asInternalName() + "." + f.name().stringValue() + ":" + f.type().stringValue());
                  case ConstantInstruction c -> fp.add("C " + c.constantValue().getClass().getSimpleName() + " " + c.constantValue());
                  case NewObjectInstruction n -> fp.add("N " + n.className().asInternalName());
                  case TypeCheckInstruction t -> fp.add("T " + t.opcode().name() + " " + t.type().asInternalName());
                  case InvokeDynamicInstruction d -> fp.add("D " + d.name().stringValue() + d.type().stringValue());
                  default -> { }
               }
            }
         });
         if (name.startsWith("lambda$")) {
            lambdas.add(fp);
         } else {
            out.put(name + mm.methodType().stringValue(), fp);
         }
      }
      // lambdas by content
      List<String> lam = new ArrayList<>();
      for (Set<String> l : lambdas) {
         lam.add(String.join("|", l));
      }
      Collections.sort(lam);
      Set<String> lamSet = new TreeSet<>();
      int k = 0;
      for (String s : lam) {
         lamSet.add((k++) + ":" + s);
      }
      out.put("<lambdas>", lamSet);
      return out;
   }

   /** java ClassCompare dump <file.class> <methodName> : prints the instructions of a method. */
   static void dump(Path file, String method) throws IOException {
      ClassModel cm = ClassFile.of().parse(Files.readAllBytes(file));
      for (MethodModel mm : cm.methods()) {
         if (mm.methodName().stringValue().equals(method)) {
            System.out.println("== " + mm.methodName().stringValue() + mm.methodType().stringValue());
            mm.code().ifPresent(code -> {
               for (CodeElement e : code) {
                  System.out.println("  " + e);
               }
            });
         }
      }
   }

   public static void main(String[] args) throws Exception {
      if (args[0].equals("dump")) {
         dump(Path.of(args[1]), args[2]);
         return;
      }
      Path a = Path.of(args[0]);
      Path b = Path.of(args[1]);
      String filter = args.length > 2 ? args[2] : "";
      List<Path> files;
      try (Stream<Path> s = Files.walk(a)) {
         files = s.filter(p -> p.toString().endsWith(".class")).sorted().toList();
      }
      int classes = 0;
      int methods = 0;
      int differing = 0;
      int missing = 0;
      for (Path pa : files) {
         String rel = a.relativize(pa).toString();
         if (!rel.contains(filter)) {
            continue;
         }
         Path pb = b.resolve(rel);
         if (!Files.exists(pb)) {
            System.out.println("MISSING in B: " + rel);
            missing++;
            continue;
         }
         classes++;
         Map<String, Set<String>> fa = fingerprints(pa);
         Map<String, Set<String>> fb = fingerprints(pb);
         Set<String> keys = new TreeSet<>(fa.keySet());
         keys.addAll(fb.keySet());
         for (String key : keys) {
            methods++;
            Set<String> sa = fa.get(key);
            Set<String> sb = fb.get(key);
            if (sa == null || sb == null) {
               if (!key.startsWith("lambda$") && !key.startsWith("access$") && !key.startsWith("$SWITCH_TABLE$")) {
                  System.out.println("METHOD ONLY IN " + (sa == null ? "B" : "A") + ": " + rel + " " + key);
                  differing++;
               }
               continue;
            }
            if (!sa.equals(sb)) {
               differing++;
               Set<String> onlyA = new TreeSet<>(sa);
               onlyA.removeAll(sb);
               Set<String> onlyB = new TreeSet<>(sb);
               onlyB.removeAll(sa);
               System.out.println("DIFF " + rel + " " + key);
               onlyA.stream().limit(6).forEach(x -> System.out.println("     - " + x));
               onlyB.stream().limit(6).forEach(x -> System.out.println("     + " + x));
            }
         }
      }
      System.out.println("compared " + classes + " classes, " + methods + " method fingerprints: " + differing + " differing, " + missing + " classes missing");
   }
}
