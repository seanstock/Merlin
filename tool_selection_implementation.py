#!/usr/bin/env python3
"""
Curriculum Tool Selection System
Dynamic tool recommendation for Grade 3 Mathematics
"""

import json
from typing import List, Dict, Any, Optional
from dataclasses import dataclass
from enum import Enum
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class Difficulty(Enum):
    BEGINNER = "beginner"
    INTERMEDIATE = "intermediate"
    ADVANCED = "advanced"
    ADAPTIVE = "adaptive"

class Category(Enum):
    ASSESSMENT = "assessment"
    INTERACTIVE_LEARNING = "interactive_learning"
    PRACTICE = "practice"
    GAME_BASED = "game_based"
    REMEDIATION = "remediation"
    EXTENSION = "extension"

@dataclass
class StudentProfile:
    """Student learning profile and current state"""
    student_id: str
    current_level: str  # "below_grade", "at_grade", "above_grade"
    learning_style: List[str]  # ["visual", "auditory", "kinesthetic"]
    attention_span: int  # minutes
    previous_tool_effectiveness: Dict[str, float]  # tool_id -> effectiveness_score
    current_struggles: List[str]  # ["place_value", "multiplication"]
    mastered_concepts: List[str]
    accessibility_needs: List[str]

@dataclass
class LearningContext:
    """Current learning context and constraints"""
    time_available: int  # minutes
    materials_available: List[str]  # ["digital", "physical", "none"]
    group_size: str  # "individual", "small_group", "whole_class"
    lesson_phase: str  # "introduction", "practice", "assessment", "review"
    standards_focus: List[str]  # ["3.OA.A.1", "3.NBT.A.2"]

@dataclass
class Tool:
    """Educational tool definition"""
    tool_id: str
    name: str
    description: str
    category: Category
    difficulty: Difficulty
    standards: List[str]
    time_required: int  # minutes
    materials: List[str]
    group_size: List[str]
    prerequisites: List[str]
    learning_objectives: List[str]
    accessibility_features: Dict[str, bool]
    effectiveness_data: Dict[str, float]  # student_profile_type -> avg_effectiveness
    setup_instructions: str
    execution_steps: List[str]
    variations: List[str]
    next_steps: List[str]

class ToolRegistry:
    """Registry of all available educational tools"""
    
    def __init__(self):
        self.tools: Dict[str, Tool] = {}
        self._load_default_tools()
    
    def _load_default_tools(self):
        """Load default set of Grade 3 math tools"""
        
        # Assessment Tools
        self.register_tool(Tool(
            tool_id="diagnostic_place_value",
            name="Place Value Diagnostic Assessment",
            description="Quickly identifies student understanding of place value concepts up to 1000. Use when starting place value unit or when students show confusion.",
            category=Category.ASSESSMENT,
            difficulty=Difficulty.BEGINNER,
            standards=["3.NBT.A.1", "3.NBT.A.2"],
            time_required=15,
            materials=["physical", "digital"],
            group_size=["individual"],
            prerequisites=[],
            learning_objectives=["identify_place_value", "understand_digit_positions"],
            accessibility_features={"visual_support": True, "auditory_support": False},
            effectiveness_data={"below_grade": 0.85, "at_grade": 0.90, "above_grade": 0.75},
            setup_instructions="Prepare number cards 1-1000, place value chart",
            execution_steps=[
                "Present 3-digit number",
                "Ask student to identify value of each digit",
                "Record responses and note error patterns"
            ],
            variations=["verbal_only", "written_only", "manipulative_support"],
            next_steps=["place_value_manipulatives", "expanded_form_practice"]
        ))
        
        # Interactive Learning Tools
        self.register_tool(Tool(
            tool_id="multiplication_arrays",
            name="Visual Multiplication Arrays",
            description="Physical or digital arrays to visualize multiplication concepts. Use when introducing multiplication or when students need concrete representation.",
            category=Category.INTERACTIVE_LEARNING,
            difficulty=Difficulty.BEGINNER,
            standards=["3.OA.A.1"],
            time_required=20,
            materials=["physical", "digital"],
            group_size=["individual", "small_group"],
            prerequisites=["counting_fluency", "addition_understanding"],
            learning_objectives=["visualize_multiplication", "understand_repeated_addition"],
            accessibility_features={"visual_support": True, "motor_adaptations": True},
            effectiveness_data={"below_grade": 0.92, "at_grade": 0.88, "above_grade": 0.70},
            setup_instructions="Provide counters, grid paper, or digital array tool",
            execution_steps=[
                "Present multiplication problem",
                "Guide student to create array representation",
                "Count total to verify answer",
                "Discuss relationship to repeated addition"
            ],
            variations=["real_world_contexts", "larger_numbers", "missing_factor_arrays"],
            next_steps=["times_table_practice", "word_problem_solving"]
        ))
        
        # Practice Tools
        self.register_tool(Tool(
            tool_id="adaptive_math_facts",
            name="Adaptive Math Facts Practice",
            description="AI-powered practice that adjusts difficulty based on student performance. Use for building fluency in basic operations.",
            category=Category.PRACTICE,
            difficulty=Difficulty.ADAPTIVE,
            standards=["3.OA.C.7"],
            time_required=15,
            materials=["digital"],
            group_size=["individual"],
            prerequisites=["basic_operation_understanding"],
            learning_objectives=["build_fluency", "increase_speed", "improve_accuracy"],
            accessibility_features={"visual_support": True, "auditory_support": True},
            effectiveness_data={"below_grade": 0.78, "at_grade": 0.85, "above_grade": 0.82},
            setup_instructions="Load adaptive practice software",
            execution_steps=[
                "Student completes initial assessment",
                "System adjusts difficulty based on performance",
                "Student practices at appropriate level",
                "System tracks progress and adjusts"
            ],
            variations=["timed_mode", "untimed_mode", "game_mode"],
            next_steps=["application_problems", "mixed_operations"]
        ))
        
        # Game-Based Tools
        self.register_tool(Tool(
            tool_id="fraction_pizza_party",
            name="Fraction Pizza Party Game",
            description="Interactive game where students partition pizzas into equal parts and identify fractions. Use when introducing fractions or for engaging practice.",
            category=Category.GAME_BASED,
            difficulty=Difficulty.INTERMEDIATE,
            standards=["3.NF.A.3", "3.G.A.2"],
            time_required=25,
            materials=["digital", "physical"],
            group_size=["small_group"],
            prerequisites=["equal_parts_understanding"],
            learning_objectives=["identify_fractions", "partition_shapes", "compare_fractions"],
            accessibility_features={"visual_support": True, "collaborative_support": True},
            effectiveness_data={"below_grade": 0.80, "at_grade": 0.88, "above_grade": 0.85},
            setup_instructions="Load game or prepare pizza fraction manipulatives",
            execution_steps=[
                "Students take turns partitioning pizzas",
                "Identify fractions created",
                "Compare different partitions",
                "Discuss equivalent fractions discovered"
            ],
            variations=["different_shapes", "equivalent_fractions", "ordering_fractions"],
            next_steps=["fraction_number_line", "fraction_word_problems"]
        ))
    
    def register_tool(self, tool: Tool):
        """Register a new tool in the registry"""
        self.tools[tool.tool_id] = tool
        logger.info(f"Registered tool: {tool.name}")
    
    def get_tool(self, tool_id: str) -> Optional[Tool]:
        """Get a specific tool by ID"""
        return self.tools.get(tool_id)
    
    def get_tools_by_category(self, category: Category) -> List[Tool]:
        """Get all tools in a specific category"""
        return [tool for tool in self.tools.values() if tool.category == category]
    
    def get_tools_by_standard(self, standard: str) -> List[Tool]:
        """Get all tools that address a specific standard"""
        return [tool for tool in self.tools.values() if standard in tool.standards]

class ToolRecommendationEngine:
    """AI-powered tool recommendation system"""
    
    def __init__(self, tool_registry: ToolRegistry):
        self.registry = tool_registry
    
    def recommend_tools(self, 
                       student_profile: StudentProfile, 
                       learning_context: LearningContext,
                       max_recommendations: int = 3) -> List[Dict[str, Any]]:
        """
        Recommend the best tools for current learning context
        
        Returns list of recommendations with reasoning
        """
        logger.info(f"Generating recommendations for student {student_profile.student_id}")
        
        # Step 1: Filter by standards alignment
        relevant_tools = []
        for standard in learning_context.standards_focus:
            relevant_tools.extend(self.registry.get_tools_by_standard(standard))
        
        # Remove duplicates
        relevant_tools = list({tool.tool_id: tool for tool in relevant_tools}.values())
        
        # Step 2: Filter by context constraints
        suitable_tools = self._filter_by_context(relevant_tools, learning_context)
        
        # Step 3: Filter by student needs
        appropriate_tools = self._filter_by_student_profile(suitable_tools, student_profile)
        
        # Step 4: Rank by predicted effectiveness
        ranked_tools = self._rank_by_effectiveness(appropriate_tools, student_profile)
        
        # Step 5: Generate recommendations with reasoning
        recommendations = []
        for i, (tool, score) in enumerate(ranked_tools[:max_recommendations]):
            recommendation = {
                "tool": tool,
                "effectiveness_score": score,
                "rank": i + 1,
                "reasoning": self._generate_reasoning(tool, student_profile, learning_context),
                "adaptations": self._suggest_adaptations(tool, student_profile),
                "setup_time": self._estimate_setup_time(tool, learning_context),
                "expected_outcomes": self._predict_outcomes(tool, student_profile)
            }
            recommendations.append(recommendation)
        
        logger.info(f"Generated {len(recommendations)} recommendations")
        return recommendations
    
    def _filter_by_context(self, tools: List[Tool], context: LearningContext) -> List[Tool]:
        """Filter tools based on contextual constraints"""
        filtered = []
        
        for tool in tools:
            # Check time constraints
            if tool.time_required > context.time_available:
                continue
            
            # Check material availability
            if not any(material in context.materials_available for material in tool.materials):
                continue
            
            # Check group size compatibility
            if context.group_size not in tool.group_size:
                continue
            
            filtered.append(tool)
        
        return filtered
    
    def _filter_by_student_profile(self, tools: List[Tool], profile: StudentProfile) -> List[Tool]:
        """Filter tools based on student profile and needs"""
        filtered = []
        
        for tool in tools:
            # Check if student has prerequisites
            if not all(prereq in profile.mastered_concepts for prereq in tool.prerequisites):
                continue
            
            # Check attention span compatibility
            if tool.time_required > profile.attention_span:
                continue
            
            # Check accessibility needs
            accessibility_match = True
            for need in profile.accessibility_needs:
                if need == "visual_support" and not tool.accessibility_features.get("visual_support", False):
                    accessibility_match = False
                    break
            
            if not accessibility_match:
                continue
            
            filtered.append(tool)
        
        return filtered
    
    def _rank_by_effectiveness(self, tools: List[Tool], profile: StudentProfile) -> List[tuple]:
        """Rank tools by predicted effectiveness for this student"""
        scored_tools = []
        
        for tool in tools:
            # Base effectiveness from historical data
            base_score = tool.effectiveness_data.get(profile.current_level, 0.5)
            
            # Adjust for previous tool effectiveness
            if tool.tool_id in profile.previous_tool_effectiveness:
                personal_score = profile.previous_tool_effectiveness[tool.tool_id]
                # Weight: 70% historical data, 30% personal data
                base_score = 0.7 * base_score + 0.3 * personal_score
            
            # Boost for learning style match
            style_boost = 0
            if "visual" in profile.learning_style and tool.accessibility_features.get("visual_support", False):
                style_boost += 0.1
            if "auditory" in profile.learning_style and tool.accessibility_features.get("auditory_support", False):
                style_boost += 0.1
            if "kinesthetic" in profile.learning_style and tool.accessibility_features.get("motor_adaptations", False):
                style_boost += 0.1
            
            # Boost for addressing current struggles
            struggle_boost = 0
            for struggle in profile.current_struggles:
                if struggle in tool.learning_objectives:
                    struggle_boost += 0.15
            
            final_score = base_score + style_boost + struggle_boost
            scored_tools.append((tool, final_score))
        
        # Sort by score (descending)
        return sorted(scored_tools, key=lambda x: x[1], reverse=True)
    
    def _generate_reasoning(self, tool: Tool, profile: StudentProfile, context: LearningContext) -> str:
        """Generate human-readable reasoning for tool recommendation"""
        reasons = []
        
        # Standards alignment
        matching_standards = [s for s in tool.standards if s in context.standards_focus]
        if matching_standards:
            reasons.append(f"Aligns with standards: {', '.join(matching_standards)}")
        
        # Student level appropriateness
        effectiveness = tool.effectiveness_data.get(profile.current_level, 0)
        if effectiveness > 0.8:
            reasons.append(f"Highly effective for {profile.current_level.replace('_', ' ')} students")
        
        # Learning style match
        if "visual" in profile.learning_style and tool.accessibility_features.get("visual_support"):
            reasons.append("Provides visual support matching student's learning style")
        
        # Addresses struggles
        struggle_match = [s for s in profile.current_struggles if s in tool.learning_objectives]
        if struggle_match:
            reasons.append(f"Addresses current struggles: {', '.join(struggle_match)}")
        
        # Time appropriateness
        if tool.time_required <= profile.attention_span:
            reasons.append("Appropriate duration for student's attention span")
        
        return "; ".join(reasons)
    
    def _suggest_adaptations(self, tool: Tool, profile: StudentProfile) -> List[str]:
        """Suggest adaptations based on student needs"""
        adaptations = []
        
        if profile.current_level == "below_grade":
            adaptations.extend(["Use simpler numbers", "Provide additional scaffolding", "Allow extra time"])
        elif profile.current_level == "above_grade":
            adaptations.extend(["Increase complexity", "Add extension activities", "Encourage peer teaching"])
        
        if profile.attention_span < tool.time_required:
            adaptations.append("Break into shorter segments")
        
        if "visual_support" in profile.accessibility_needs:
            adaptations.append("Provide visual aids and graphic organizers")
        
        return adaptations
    
    def _estimate_setup_time(self, tool: Tool, context: LearningContext) -> int:
        """Estimate setup time in minutes"""
        base_time = 2  # Base setup time
        
        if "physical" in tool.materials:
            base_time += 3  # Physical materials take longer
        
        if context.group_size == "whole_class":
            base_time += 2  # More setup for larger groups
        
        return base_time
    
    def _predict_outcomes(self, tool: Tool, profile: StudentProfile) -> List[str]:
        """Predict learning outcomes"""
        outcomes = []
        
        effectiveness = tool.effectiveness_data.get(profile.current_level, 0.5)
        
        if effectiveness > 0.8:
            outcomes.append("High probability of concept mastery")
        elif effectiveness > 0.6:
            outcomes.append("Good progress expected")
        else:
            outcomes.append("May need additional support")
        
        outcomes.extend([f"Develop: {obj}" for obj in tool.learning_objectives[:2]])
        
        return outcomes

# Example Usage
def main():
    """Demonstrate the tool recommendation system"""
    
    # Initialize system
    registry = ToolRegistry()
    engine = ToolRecommendationEngine(registry)
    
    # Create sample student profile
    student = StudentProfile(
        student_id="sarah_j_123",
        current_level="below_grade",
        learning_style=["visual", "kinesthetic"],
        attention_span=15,
        previous_tool_effectiveness={"multiplication_arrays": 0.85},
        current_struggles=["multiplication", "place_value"],
        mastered_concepts=["counting_fluency", "addition_understanding"],
        accessibility_needs=["visual_support"]
    )
    
    # Create learning context
    context = LearningContext(
        time_available=20,
        materials_available=["digital", "physical"],
        group_size="individual",
        lesson_phase="introduction",
        standards_focus=["3.OA.A.1", "3.NBT.A.1"]
    )
    
    # Get recommendations
    recommendations = engine.recommend_tools(student, context)
    
    # Display results
    print("\n" + "="*60)
    print("TOOL RECOMMENDATIONS")
    print("="*60)
    print(f"Student: {student.student_id}")
    print(f"Level: {student.current_level}")
    print(f"Time Available: {context.time_available} minutes")
    print(f"Standards Focus: {', '.join(context.standards_focus)}")
    print("\n")
    
    for i, rec in enumerate(recommendations, 1):
        tool = rec["tool"]
        print(f"{i}. {tool.name}")
        print(f"   Category: {tool.category.value}")
        print(f"   Time Required: {tool.time_required} minutes")
        print(f"   Effectiveness Score: {rec['effectiveness_score']:.2f}")
        print(f"   Reasoning: {rec['reasoning']}")
        print(f"   Suggested Adaptations: {', '.join(rec['adaptations'])}")
        print(f"   Expected Outcomes: {', '.join(rec['expected_outcomes'])}")
        print(f"   Setup Instructions: {tool.setup_instructions}")
        print("\n")

if __name__ == "__main__":
    main() 