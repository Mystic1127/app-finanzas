import { enableFirebaseTelemetry } from "@genkit-ai/firebase";
import { googleAI } from "@genkit-ai/google-genai";
import { genkit, z } from "genkit";
import { initializeApp } from "firebase-admin/app";
import { defineSecret } from "firebase-functions/params";
import { onCallGenkit } from "firebase-functions/https";

const geminiApiKey = defineSecret("GEMINI_API_KEY");

initializeApp();
void enableFirebaseTelemetry({
  disableLoggingInputAndOutput: true,
});

const ai = genkit({
  plugins: [googleAI()],
});

const MonthlyAggregateSchema = z
  .object({
    year: z.number().int().min(2020).max(2100),
    month: z.number().int().min(1).max(12),
    currency: z.string().trim().min(3).max(8),
    totalIncome: z.number().finite().min(0),
    totalExpense: z.number().finite().min(0),
    currentBalance: z.number().finite(),
    estimatedMonthEndBalance: z.number().finite(),
    projectedExpense: z.number().finite().min(0),
    budgetTotal: z.number().finite().min(0),
    budgetUsed: z.number().finite().min(0),
    budgetRemaining: z.number().finite(),
    topExpenseCategoryName: z.string().trim().max(40).nullable().optional(),
    topExpenseCategoryAmount: z.number().finite().min(0),
    activeGoalsCount: z.number().int().min(0).max(50),
    activeGoalsTotalTarget: z.number().finite().min(0),
    activeGoalsSavedAmount: z.number().finite().min(0),
    recurringExpensesTotal: z.number().finite().min(0).nullable().optional(),
    financialScore: z.number().int().min(0).max(100).nullable().optional(),
    riskHints: z.array(z.string().trim().min(1).max(120)).max(4).default([]),
  })
  .strict();

const FinancialAssistantOutputSchema = z
  .object({
    summary: z.string().trim().min(8).max(220),
    suggestedSavingAmount: z.number().finite().min(0),
    riskLevel: z.enum(["LOW", "MEDIUM", "HIGH"]),
    riskLabel: z.enum(["Estable", "Atento", "Riesgo alto"]),
    recommendedAction: z.string().trim().min(8).max(220),
    alerts: z.array(z.string().trim().min(1).max(160)).max(4),
    positiveInsight: z.string().trim().min(8).max(180),
    mainConcern: z.string().trim().min(8).max(180),
    generatedBy: z.literal("ai"),
  })
  .strict();

type MonthlyAggregate = z.infer<typeof MonthlyAggregateSchema>;

function promptInput(input: MonthlyAggregate): string {
  return JSON.stringify({
    year: input.year,
    month: input.month,
    currency: input.currency,
    totalIncome: input.totalIncome,
    totalExpense: input.totalExpense,
    currentBalance: input.currentBalance,
    estimatedMonthEndBalance: input.estimatedMonthEndBalance,
    projectedExpense: input.projectedExpense,
    budgetTotal: input.budgetTotal,
    budgetUsed: input.budgetUsed,
    budgetRemaining: input.budgetRemaining,
    topExpenseCategoryName: input.topExpenseCategoryName ?? null,
    topExpenseCategoryAmount: input.topExpenseCategoryAmount,
    activeGoalsCount: input.activeGoalsCount,
    activeGoalsTotalTarget: input.activeGoalsTotalTarget,
    activeGoalsSavedAmount: input.activeGoalsSavedAmount,
    recurringExpensesTotal: input.recurringExpensesTotal ?? null,
    financialScore: input.financialScore ?? null,
    riskHints: input.riskHints,
  });
}

export const financialAssistantFlow = ai.defineFlow(
  {
    name: "financialAssistant",
    inputSchema: MonthlyAggregateSchema,
    outputSchema: FinancialAssistantOutputSchema,
  },
  async (input) => {
    const started = Date.now();
    console.info("financialAssistant.start", {
      year: input.year,
      month: input.month,
      hasIncome: input.totalIncome > 0,
      hasExpense: input.totalExpense > 0,
    });

    const { output } = await ai.generate({
      model: googleAI.model("gemini-2.5-flash"),
      system:
        "Eres un asistente financiero para una app de presupuesto personal. " +
        "Responde solo con JSON valido segun el esquema. Usa lenguaje breve, claro, practico y no alarmista. " +
        "No inventes datos, no prometas resultados, no des asesoramiento financiero profesional. " +
        "Basate solo en las metricas agregadas recibidas. No menciones transacciones privadas ni datos personales.",
      prompt:
        "Analiza estas metricas financieras agregadas del mes y devuelve una recomendacion estructurada. " +
        "No solicites datos adicionales. No incluyas markdown. Datos agregados:\n" +
        promptInput(input),
      output: { schema: FinancialAssistantOutputSchema },
      config: {
        temperature: 0.2,
        maxOutputTokens: 700,
      },
    });

    if (!output) {
      throw new Error("Gemini response did not satisfy the assistant schema.");
    }

    console.info("financialAssistant.success", {
      year: input.year,
      month: input.month,
      durationMs: Date.now() - started,
      riskLevel: output.riskLevel,
    });
    return output;
  },
);

export const financialAssistant = onCallGenkit(
  {
    secrets: [geminiApiKey],
    timeoutSeconds: 25,
    memory: "512MiB",
    authPolicy: (auth) => Boolean(auth?.uid),
  },
  financialAssistantFlow,
);
