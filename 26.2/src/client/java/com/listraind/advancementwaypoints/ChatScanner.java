package com.listraind.advancementwaypoints;

import com.listraind.advancementwaypoints.config.ModConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public class ChatScanner {
   private static final Pattern NUMBER_PATTERN = Pattern.compile("-\\d+|\\d+");

   public static Component scanAndHighlight(Component message) {
      if (message == null) {
         return null;
      } else if (!ModConfig.getInstance().isEnableChatScanner()) {
         return message;
      } else {
         try {
            return processComponent(message);
         } catch (Exception e) {
            e.printStackTrace();
            return message;
         }
      }
   }

   private static Component processComponent(Component component) {
      ComponentContents text = component.getContents();
      MutableComponent result;
      if (text instanceof PlainTextContents plainText) {
         String rawText = plainText.text();
         if (!rawText.isEmpty() && containsDigits(rawText)) {
            result = processText(rawText, component.getStyle());
         } else {
            result = Component.literal(rawText).setStyle(component.getStyle());
         }
      } else {
         text = component.getContents();
         if (text instanceof TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            Object[] newArgs = new Object[args.length];

            for(int i = 0; i < args.length; ++i) {
               Object arg = args[i];
               if (arg instanceof Component argComp) {
                  newArgs[i] = processComponent(argComp);
               } else if (arg instanceof String argStr) {
                  if (containsDigits(argStr)) {
                     newArgs[i] = processText(argStr, component.getStyle());
                  } else {
                     newArgs[i] = argStr;
                  }
               } else {
                  newArgs[i] = arg;
               }
            }

            result = Component.translatableWithFallback(translatable.getKey(), translatable.getFallback(), newArgs).setStyle(component.getStyle());
         } else {
            result = component.copy();
            result.getSiblings().clear();
         }
      }

      for(Component sibling : component.getSiblings()) {
         result.append(processComponent(sibling));
      }

      return result;
   }

   private static boolean containsDigits(String text) {
      for(int i = 0; i < text.length(); ++i) {
         if (Character.isDigit(text.charAt(i))) {
            return true;
         }
      }

      return false;
   }

   private static MutableComponent processText(String text, Style baseStyle) {
      List<NumberMatch> numbers = new ArrayList();
      Matcher matcher = NUMBER_PATTERN.matcher(text);

      while(matcher.find()) {
         numbers.add(new NumberMatch(matcher.start(), matcher.end(), matcher.group()));
      }

      if (numbers.isEmpty()) {
         return Component.literal(text).setStyle(baseStyle);
      } else {
         List<CoordRange> coordsList = new ArrayList();

         int seqEnd;
         for(int i = 0; i < numbers.size(); i = seqEnd + 1) {
            for(seqEnd = i; seqEnd + 1 < numbers.size(); ++seqEnd) {
               String sep = text.substring(((NumberMatch)numbers.get(seqEnd)).end, ((NumberMatch)numbers.get(seqEnd + 1)).start);
               if (!isCoordDelimiter(sep)) {
                  break;
               }
            }

            int count = seqEnd - i + 1;
            if (count == 2 || count == 3) {
               int matchStart = ((NumberMatch)numbers.get(i)).start;
               int matchEnd = ((NumberMatch)numbers.get(seqEnd)).end;
               StringBuilder cmdBuilder = new StringBuilder("/navigate");

               for(int k = i; k <= seqEnd; ++k) {
                  cmdBuilder.append(" ").append(((NumberMatch)numbers.get(k)).val);
               }

               coordsList.add(new CoordRange(matchStart, matchEnd, cmdBuilder.toString()));
            }
         }

         if (coordsList.isEmpty()) {
            return Component.literal(text).setStyle(baseStyle);
         } else {
            MutableComponent root = Component.empty();
            seqEnd = 0;

            for(CoordRange range : coordsList) {
               if (range.start > seqEnd) {
                  String normalText = text.substring(seqEnd, range.start);
                  root.append(Component.literal(normalText).setStyle(baseStyle));
               }

               String coordText = text.substring(range.start, range.end);
               Style coordStyle = baseStyle.withUnderlined(true).withClickEvent(new ClickEvent.SuggestCommand(range.command)).withHoverEvent(new HoverEvent.ShowText(Component.literal("Нажмите, чтобы ввести " + range.command)));
               root.append(Component.literal(coordText).setStyle(coordStyle));
               seqEnd = range.end;
            }

            if (seqEnd < text.length()) {
               root.append(Component.literal(text.substring(seqEnd)).setStyle(baseStyle));
            }

            return root;
         }
      }
   }

   private static boolean isCoordDelimiter(String sep) {
      if (sep.length() > 20) {
         return false;
      } else {
         for(int i = 0; i < sep.length(); ++i) {
            char c = sep.charAt(i);
            if (!Character.isWhitespace(c) && c != ',' && c != '/' && c != ':' && c != '~' && c != '=' && c != 'x' && c != 'X' && c != 'y' && c != 'Y' && c != 'z' && c != 'Z') {
               return false;
            }
         }

         return true;
      }
   }

   private static class NumberMatch {
      final int start;
      final int end;
      final String val;

      NumberMatch(int start, int end, String val) {
         this.start = start;
         this.end = end;
         this.val = val;
      }
   }

   private static class CoordRange {
      final int start;
      final int end;
      final String command;

      CoordRange(int start, int end, String command) {
         this.start = start;
         this.end = end;
         this.command = command;
      }
   }
}
